package com.atyichen.project.yiojcodesandbox;

import com.atyichen.project.yiojcodesandbox.model.JudgeInfo;

import java.util.ArrayList;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import com.atyichen.project.yiojcodesandbox.model.ExecuteCodeRequest;
import com.atyichen.project.yiojcodesandbox.model.ExecuteCodeResponse;
import com.atyichen.project.yiojcodesandbox.model.ExecuteMessage;
import com.atyichen.project.yiojcodesandbox.utils.ProcessUtils;
import org.springframework.util.StopWatch;

public class JavaNativeCodeSandBox implements CodeSandBox {
    private static final String GLOBAL_CODE_DIR_NAME = "tempCode";
    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

//    /**
//     * 没有使用ProcessUtil 代码冗余
//     * @param executeCodeRequest
//     * @return
//     */
//    @Override
//    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
//        List<String> inputList = executeCodeRequest.getInputList();
//        String code = executeCodeRequest.getCode();
//        String language = executeCodeRequest.getLanguage();
//
//        // 1. 将用户提交的代码保存为文件
//        // 方法会返回一个表示当前工作目录的 String，并将其赋值给变量 userDir。 比如之后其他项目调用代码沙箱， 那么就返回其他项目当前所在的路径
//        String userDir = System.getProperty("user.dir");
//        // 创建文件保存的目录
//        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
//        // 判断全局路径是否存在
//        if (!FileUtil.exist(globalCodePathName)) {
//            FileUtil.mkdir(globalCodePathName);
//        }
//        // 保存用户提交的代码
//        // 将用户的代码隔离保存
//        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
//        // 实际存放的文件的目录
//        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
//        // 写入文件
//        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
//
//        // 2. 编译代码， 得到class文件
//        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
//
//        // 使用 runtime执行进程
//        try {
//            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
//            // 等待进程执行完 获取错误码
//            int exitValue = compileProcess.waitFor();
//            // 正常退出
//            if (exitValue == 0) {
//                System.out.println("编译成功");
//                // 获取 编译后 输出到控制台的信息
//                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(compileProcess.getInputStream()));
//                StringBuilder compileOutputStringBuilder = new StringBuilder();
//
//                // 逐行读取
//                String compileOutputLine;
//                while ((compileOutputLine = bufferedReader.readLine()) != null) {
//                    compileOutputStringBuilder.append(compileOutputLine);
//                }
//                System.out.println(compileOutputStringBuilder);
//            } else {
//                System.out.println("编译失败");
//                // 获取 编译后 输出到控制台的信息
//                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(compileProcess.getInputStream()));
//                StringBuilder compileOutputStringBuilder = new StringBuilder();
//
//                // 逐行读取
//                String compileOutputLine;
//                while ((compileOutputLine = bufferedReader.readLine()) != null) {
//                    compileOutputStringBuilder.append(compileOutputLine);
//                }
//
//                // 获取 编译后 进程的错误输出
//                BufferedReader errorBufferedReader = new BufferedReader(new InputStreamReader(compileProcess.getErrorStream()));
//                StringBuilder errorCompileOutputStringBuilder = new StringBuilder();
//
//                // 逐行读取
//                String errorCompileOutputLine;
//                while ((errorCompileOutputLine = errorBufferedReader.readLine()) != null) {
//                    errorCompileOutputStringBuilder.append(errorCompileOutputLine);
//                }
//
//                System.out.println(compileOutputStringBuilder);
//                System.out.println(errorCompileOutputStringBuilder);
//            }
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//        return null;
//    }

    public static void main(String[] args) {
        CodeSandBox testCodeSandBox = new JavaNativeCodeSandBox();
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

    /**
     * 使用ProcessUtil
     *
     * @param executeCodeRequest
     * @return
     */
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {

        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();

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

        // 2. 编译代码， 得到class文件
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileCmd, "编译代码");
        System.out.println(executeMessage);
        if (executeMessage.getExitValue() == 1) {
            ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
            executeCodeResponse.setStatus(0);
            return executeCodeResponse;
        }

        // 3. 执行代码 得到输出结果
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            String runCmd = String.format("java -cp %s Main %s", userCodeParentPath, inputArgs);
            ExecuteMessage runExecuteMessage = ProcessUtils.runProcessAndGetMessage(runCmd, "执行代码");
            executeMessageList.add(runExecuteMessage);
            System.out.println(runExecuteMessage);
        }

        // 4. 收集整理输出结果
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();

        List<String> outputList = new ArrayList<>();
        // 程序执行时间 记录一个最大值
        Long maxTime = 0L;
        for (ExecuteMessage executeMessageItem : executeMessageList) {
            int exitValue = executeMessageItem.getExitValue();
            String message = executeMessageItem.getMessage();
            String errorMessage = executeMessageItem.getErrorMessage();
            Long time = executeMessageItem.getTime();
            if (time != null)
                maxTime = Math.max(maxTime, time);
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
        judgeInfo.setMemory(0L);
        judgeInfo.setTime(maxTime);
        executeCodeResponse.setJudgeInfo(judgeInfo);
        System.out.println(executeCodeResponse);

        // 5. 清理文件 执行完以后 源码和编译后的class都可以删除
        if (userCodeFile.getParentFile() != null) {
            boolean del = FileUtil.del(userCodeFile.getParentFile());
            if (del) System.out.println("删除成功");
            else System.out.println("删除失败");
        }

        // 6. 错误处理， 提升代码健壮性
        return executeCodeResponse;
    }

    /**
     * 抛出异常就可以返回一个错误类
     *
     * @param e
     * @return
     */
//    try {
//
//    }catch (Exception e) {
//        return getErrorResponse(e);
//    }
    private ExecuteCodeResponse getErrorResponse(Throwable e) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        // 表示代码沙箱错误
        executeCodeResponse.setStatus(2);
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;
    }
}
