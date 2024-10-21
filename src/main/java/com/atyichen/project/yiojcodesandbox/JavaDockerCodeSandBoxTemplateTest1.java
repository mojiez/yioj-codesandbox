package com.atyichen.project.yiojcodesandbox;

import com.atyichen.project.yiojcodesandbox.model.ExecuteCodeRequest;
import com.atyichen.project.yiojcodesandbox.model.ExecuteCodeResponse;
import com.atyichen.project.yiojcodesandbox.model.ExecuteMessage;

import java.io.Closeable;
import java.io.File;
import java.util.Arrays;
import java.util.List;

public class JavaDockerCodeSandBoxTemplateTest1 extends JavaCodeSandBoxTemplate {
    @Override
    public ExecuteMessage compileCode(File codeFile) {
        System.out.println("test1编译文件啦啦啦");
        return super.compileCode(codeFile);
    }

    @Override
    public String createAndRunContainer(File userCodeFile) {
        System.out.println("test1 创建容器 啦啦啦");
        return super.createAndRunContainer(userCodeFile);
    }

    @Override
    public Boolean listenGetMemory(String containerId, Runnable callback) {
        System.out.println("test1 监听内存 啦啦啦");
        return super.listenGetMemory(containerId, callback);
    }

    @Override
    public List<ExecuteMessage> dockerExec(Closeable closeableRef, List<String> inputList, String containerId) {
        System.out.println("test1 docker exec 啦啦啦");
        return super.dockerExec(closeableRef, inputList, containerId);
    }

    @Override
    public ExecuteCodeResponse getCodeResponse(List<ExecuteMessage> executeMessageList) {
        System.out.println("test1 getResponse 啦啦啦");
        return super.getCodeResponse(executeMessageList);
    }

    @Override
    public void clearFileContainer(File userCodeFile, String containerId) {
        System.out.println("test1 删除 啦啦啦");
        super.clearFileContainer(userCodeFile, containerId);
    }

    @Override
    public File writeCode(String code) {
        System.out.println("test1 写文件啦啦啦");
        return super.writeCode(code);
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return super.executeCode(executeCodeRequest);
    }


    public static void main(String[] args) {
        CodeSandBox testCodeSandBox = new JavaDockerCodeSandBoxTemplateTest1();
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
