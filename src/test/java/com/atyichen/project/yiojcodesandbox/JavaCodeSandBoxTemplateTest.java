package com.atyichen.project.yiojcodesandbox;

import com.atyichen.project.yiojcodesandbox.model.ExecuteCodeRequest;
import com.atyichen.project.yiojcodesandbox.model.ExecuteCodeResponse;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class JavaCodeSandBoxTemplateTest {

    @Test
    void executeCode() {
        //    public static void main(String[] args) {
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