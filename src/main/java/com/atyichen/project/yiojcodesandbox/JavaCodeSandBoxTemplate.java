package com.atyichen.project.yiojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ArrayUtil;
import com.atyichen.project.yiojcodesandbox.model.ExecuteCodeRequest;
import com.atyichen.project.yiojcodesandbox.model.ExecuteCodeResponse;
import com.atyichen.project.yiojcodesandbox.model.ExecuteMessage;
import com.atyichen.project.yiojcodesandbox.model.JudgeInfo;
import com.atyichen.project.yiojcodesandbox.utils.ProcessUtils;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class JavaCodeSandBoxTemplate implements CodeSandBox {
    private static final String GLOBAL_CODE_DIR_NAME = "tempCode";
    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";
    private static final String JAVA_JDK_IMAGE = "openjdk:8-alpine";
    private static final boolean hasPull = true;
    private static final DockerClient dockerClient;
    private static final Closeable[] closeableRef = {null};
    private static final Long[] maxMemory = {0L};

    static {
        dockerClient = DockerClientBuilder.getInstance().build();
    }

    /**
     * 将用户提交的代码保存为文件
     *
     * @param code
     * @return
     */
    public File writeCode(String code) {
        // 1. 将用户提交的代码保存为文件
        // 方法会返回一个表示当前工作目录的 String，并将其赋值给变量 userDir。 比如之后其他项目调用代码沙箱， 那么就返回其他项目当前所在的路径
        String userDir = System.getProperty("user.dir");
        // 创建文件保存的目录
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        // 判断全局路径是否存在
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }
        // 保存用户提交的代码
        // 将用户的代码隔离保存
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        // 实际存放的文件的目录
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        // 写入文件
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        return userCodeFile;
    }

    /**
     * 编译代码
     *
     * @param codeFile
     * @return
     */
    public ExecuteMessage compileCode(File codeFile) {
        String compileCmd = String.format("javac -encoding utf-8 %s", codeFile.getAbsolutePath());
        ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileCmd, "编译代码");
        System.out.println(executeMessage);
        if (executeMessage.getExitValue() == 1) {
            throw new RuntimeException("编译代码错误");
        }
        return executeMessage;
    }

    public Boolean pullJavaEnvImage() {
//         (3). 一次性 拉取java环境的镜像
//         获取默认的Docker Client
        PullImageCmd pullImageCmd = dockerClient.pullImageCmd(JAVA_JDK_IMAGE);
        // 接收回调
        PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
            @Override
            public void onNext(PullResponseItem item) {
                System.out.println("下载镜像: " + item.getStatus());
                super.onNext(item);
            }
        };
        try {
            pullImageCmd.exec(pullImageResultCallback).awaitCompletion();
        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
            log.error(e.getMessage());
            return false;
        }
        System.out.println("下载完成");
        return true;
    }

    public String createAndRunContainer(File userCodeFile) {
        // 3. 把编译好的文件上传到容器环境内（没必要在容器中编译）
        // 要创建一个可交互的容器， 能接受多次输入并且输出
        // 创建容器挂载目录
        String userDir = System.getProperty("user.dir");
        // 创建文件保存的目录
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        // 将用户的代码隔离保存
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();

        HostConfig hostConfig = new HostConfig();
        hostConfig.setBinds(new Bind(userCodeParentPath, new Volume("/yioj")));
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(JAVA_JDK_IMAGE);
        CreateContainerResponse createContainerResponse = containerCmd.withHostConfig(hostConfig).withAttachStderr(true).withAttachStdin(true).withAttachStdout(true).withTty(true).exec();
        // 这里只是决定了容器是否允许捕获输入输出流，以及是否支持伪终端（TTY）交互。
        // 它为容器本身提供了处理输入、输出、错误流和交互式会话的能力，但它并不实际执行命令的输入输出捕获。
        // 似于一个前提条件，决定了容器能不能与外部进程交互，以及是否能捕获或发送数据。如果没有这些设置，
        // 即使你在后续执行命令时附加了输入输出流，也无法捕获这些流，因为容器本身不支持。
        // 总结：第一种方式仅是配置容器具备捕获输入输出流的能力。
        String containerId = createContainerResponse.getId();
        System.out.println("创建容器id: " + containerId);
        // 启动容器
        dockerClient.startContainerCmd(containerId).exec();
        return containerId;
    }

    public Boolean listenGetMemory(String containerId, Runnable callback) {
        StatsCmd statsCmd = dockerClient.statsCmd(containerId);
        // 知识点： 为什么在匿名内部类中修改值 要定义成这种形式？
        // 内部类如果调用了方法中的变量，那么该变量必须申明为final 或 effective final类型，如果不申明，则编译就会出错。
        // 如果你想在匿名内部类中修改外部的值，就需要通过引用类型来实现。
        // 引用类型被声明为final 可以改变其本身的属性，但是不能修改它指向的地址
        // 为什么要数组， 我修改final Long的属性不行吗？
        // Long 是 Java 的一个包装类，它是不可变的。所有的 Java 包装类（如 Integer、Double、Boolean 等）都遵循不可变性设计模式。不可变类的特点是：
        statsCmd.exec(new ResultCallback<Statistics>() {
            @Override
            public void onStart(Closeable closeable) {
                // 保存 closeable 引用，供以后关闭时使用
                System.out.println("onStart 被调用了");
                if (closeable != null) {
                    closeableRef[0] = closeable;
                    System.out.println("Closeable 已赋值");
                    if (callback != null) callback.run();
                } else {
                    System.out.println("未能获取到 Closeable 引用！");
                }
            }

            @Override
            public void onNext(Statistics object) {
                // 统计内存占用
                System.out.println("内存占用: " + object.getMemoryStats().getUsage());
                if (object.getMemoryStats().getUsage() != null)
                    maxMemory[0] = Math.max(object.getMemoryStats().getUsage(), maxMemory[0]);
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onComplete() {
                System.out.println("关闭统计！");
            }

            @Override
            public void close() throws IOException {
            }
        });
        return true;
    }

    public List<ExecuteMessage> dockerExec(Closeable closeableRef, List<String> inputList, String containerId) {
        // docker exec 38fcdc27d789319445234fbf2f260101b25cfab26da4e88cdb6ec6aa394461bf java -version
        List<String> execCmdIdList = new ArrayList<>();
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        // 初始化执行任务
        for (String inputArgs : inputList) {
            String[] inputArgsArray = inputArgs.split(" ");
            String[] cmdArray = ArrayUtil.append(new String[]{"java", "-cp", "/yioj", "Main"}, inputArgsArray);
            System.out.println("cmdArray: " + Arrays.toString(cmdArray));
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId).withCmd(cmdArray).withAttachStderr(true).withAttachStdin(true).withAttachStdout(true)
                    // 实际行为，决定在执行命令时是否 真的捕获输入输出。
                    .exec();
            execCmdIdList.add(execCreateCmdResponse.getId());
        }

        // 真正执行
        for (String execCmdId : execCmdIdList) {
            ExecuteMessage executeDockerMessage = new ExecuteMessage();
            StringBuilder execOutputStringBuilder = new StringBuilder();
            StringBuilder execErrOutputStringBuilder = new StringBuilder();
            // 定义执行命令的回调 这个回调不能共享 不然第一次循环阻塞中，进行第二次循环， 回调函数可能会来不及处理
            ResultCallback.Adapter<Frame> startCmdCallBack = new ResultCallback.Adapter<Frame>() {
                @Override
                public void onComplete() {
                    if (execErrOutputStringBuilder.length() == 0) {
                        // 说明是正确运行的
                        executeDockerMessage.setMessage(execOutputStringBuilder.toString());
                        executeDockerMessage.setExitValue(0);
                    } else {
                        executeDockerMessage.setErrorMessage(execErrOutputStringBuilder.toString());
                        executeDockerMessage.setExitValue(1);
                    }
                    super.onComplete();
                }

                @Override
                // onNext方法每次接收到新数据时被调用， onNext会在Docker容器输出一段新的数据时触发，并将该数据作为Frame对象传给你
                public void onNext(Frame object) {
                    // Frame是docker中用于表示容器命令执行时产生的输出或输入的数据单元
                    // 每个 Frame 是输出流的一个片段：当你在 Docker 容器中运行命令时，如果输出比较大，
                    // Docker 会将数据分段，以帧（Frame）的形式返回。每个帧携带一部分输出数据，直到全部输出完毕。
                    StreamType streamType = object.getStreamType();
                    if (StreamType.STDERR.equals(streamType)) {
                        System.out.println("输出错误结果: " + new String(object.getPayload()));
                        execErrOutputStringBuilder.append(new String(object.getPayload()));
                    } else {
                        System.out.println("输出结果: " + new String(object.getPayload()));
                        execOutputStringBuilder.append(new String(object.getPayload()));
                    }
                    super.onNext(object);
                }

            };

            try {

                StopWatch stopWatch = new StopWatch();
                stopWatch.start();
                dockerClient.execStartCmd(execCmdId).exec(startCmdCallBack).awaitCompletion();
                stopWatch.stop();
                long time = stopWatch.getLastTaskTimeMillis();
                executeDockerMessage.setTime(time);

            } catch (InterruptedException e) {
                System.out.println("docker exec执行异常");
                throw new RuntimeException(e);
            }
            executeMessageList.add(executeDockerMessage);
        }
        // 关闭统计
        try {
            if (closeableRef != null) {
                System.out.println("我要关闭统计了！");
                closeableRef.close();  // 手动关闭监控，onComplete() 会被调用
            }
        } catch (IOException e) {
            System.out.println("关闭统计失败");
            throw new RuntimeException(e);
        }
        return executeMessageList;
    }

    public ExecuteCodeResponse getCodeResponse(List<ExecuteMessage> executeMessageList) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();

        List<String> outputList = new ArrayList<>();
        // 程序执行时间 记录一个最大值
        Long maxTime = 0L;
        System.out.println("List Size: " + executeMessageList.size());
        for (ExecuteMessage executeMessageItem : executeMessageList) {
            System.out.println("executeMessageItem: " + executeMessageItem);
            int exitValue = executeMessageItem.getExitValue();
            String message = executeMessageItem.getMessage();
            String errorMessage = executeMessageItem.getErrorMessage();
            Long time = executeMessageItem.getTime();
            if (time != null) maxTime = Math.max(maxTime, time);
            if (exitValue == 1) {
                // 1是不对的
                executeCodeResponse.setMessage(errorMessage);
                executeCodeResponse.setStatus(3); // todo 定义枚举值 表示代码经过codeBox的最终状态(编译成功 执行成功)
                return executeCodeResponse;
            }
            outputList.add(message);
        }
        // 执行到这里 说明全部执行成功
        executeCodeResponse.setOutputList(outputList);
        executeCodeResponse.setMessage("成功");
        executeCodeResponse.setStatus(1);

        // todo 设置judgeInfo
        JudgeInfo judgeInfo = new JudgeInfo();
        // 这里的Message表示 针对这道题目的具体情况 ac wa TLE 等等 这个等到外层再去定义 这里只需要set memory和time
        judgeInfo.setMessage("");
        judgeInfo.setMemory(maxMemory[0]);
        judgeInfo.setTime(maxTime);
        executeCodeResponse.setJudgeInfo(judgeInfo);
        System.out.println(executeCodeResponse);
        return executeCodeResponse;
    }

    public void clearFileContainer(File userCodeFile, String containerId) {
        if (userCodeFile.getParentFile() != null) {
            boolean del = FileUtil.del(userCodeFile.getParentFile());
            if (del) System.out.println("删除成功");
            else System.out.println("删除失败");
        }
        // 删除创建的docker容器
        InspectContainerResponse response = dockerClient.inspectContainerCmd(containerId).exec();
        InspectContainerResponse.ContainerState state = response.getState();

        // 获取具体的状态信息，比如是否运行
        String containerState = state.getStatus(); // 这将返回容器的状态，如 "running", "exited", "created"

        // 检查容器状态，如果不是exited状态（停止状态），处理不同情况
        if ("running".equalsIgnoreCase(containerState)) {
            // 容器正在运行，先停止它
            dockerClient.stopContainerCmd(containerId).exec();
            System.out.println("容器已停止: " + containerId);
        } else if ("created".equalsIgnoreCase(containerState)) {
            // 容器是created状态，不需要停止，直接删除
            System.out.println("容器处于Created状态: " + containerId);
        } else {
            System.out.println("容器已停止或未运行: " + containerId);
        }

        // 删除容器
        dockerClient.removeContainerCmd(containerId).exec();
        System.out.println("容器已删除: " + containerId);
        return;
    }

    /**
     * @param executeCodeRequest
     * @return
     */
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        /**
         * 1. 把用户的代码保存为文件
         * 2. 编译代码，得到 class 文件
         * 3. 把编译好的文件上传到容器环境内（没必要在容器中编译）
         * 4. 在容器中执行代码，得到输出结果
         * 5. 收集整理输出结果
         * 6. 文件清理，释放空间
         * 7. 错误处理，提升程序健壮性
         */

        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();

        // 1. 将用户提交的代码保存为文件
        File userCodeFile = writeCode(code);

        // 2. 编译代码， 得到class文件
        ExecuteMessage executeMessage = compileCode(userCodeFile);
        log.info("编译后的信息: " + String.valueOf(executeMessage));

        // (3). 一次性 拉取java环境的镜像
        if (!hasPull) {
            Boolean isPull = pullJavaEnvImage();
            if (!isPull) return getErrorResponseNoThr();
        }

        // 3. 把编译好的文件上传到容器环境内（没必要在容器中编译）
        String containerId = createAndRunContainer(userCodeFile);

        // 3.5 获取占用的内存（类似监听）
        final List<ExecuteMessage>[] executeMessageList = new List[]{new ArrayList<>()};
        Boolean isListen = listenGetMemory(containerId, () -> {
            System.out.println("closeableRef: " + closeableRef[0]);
            executeMessageList[0] = dockerExec(closeableRef[0], inputList, containerId);
        });

        // 4. 执行代码 操作已启动的容器（用3.5的函数来调用）

        /**
         * 这种写法是错误的
         * 这两个操作是异步的，closeableRef[0]还没赋值就被传入作为dockerExec的参数了
         */
//        System.out.println("closeableRef" + closeableRef[0]);
//        List<ExecuteMessage> executeMessageList = dockerExec(closeableRef[0], inputList, containerId);

        // 5. 收集整理输出结果
        ExecuteCodeResponse executeCodeResponse = getCodeResponse(executeMessageList[0]);

        // 6. 清理文件 执行完以后 源码和编译后的class都可以删除
        clearFileContainer(userCodeFile, containerId);
        return executeCodeResponse;
    }

    /**
     * 抛出异常就可以返回一个错误类
     *
     * @param e
     * @return
     */
    private ExecuteCodeResponse getErrorResponse(Throwable e) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        // 表示代码沙箱错误
        executeCodeResponse.setStatus(2);
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;
    }

    private ExecuteCodeResponse getErrorResponseNoThr() {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage("error");
        // 表示代码沙箱错误
        executeCodeResponse.setStatus(2);
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;
    }

    public static void main(String[] args) {
        CodeSandBox testCodeSandBox = new JavaCodeSandBoxTemplate();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "2 3", "4 5"));
        executeCodeRequest.setCode("public class Main {\n" +
                "    public static void main(String[] args) {\n" +
                "        int a = Integer.parseInt(args[0]);\n" +
                "        int b = Integer.parseInt(args[1]);\n" +
                "        System.out.println(\"结果: \" + (a + b));\n" +
                "    }\n" +
                "}");
        executeCodeRequest.setLanguage("java");

        ExecuteCodeResponse executeCodeResponse = testCodeSandBox.executeCode(executeCodeRequest);
    }
}

