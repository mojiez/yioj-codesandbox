package com.atyichen.project.yiojcodesandbox.dangercode;

import com.atyichen.project.yiojcodesandbox.security.DefaultSecurityManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class RunMuma {
    /**
     * 运行其他程序（比如危险木马）
     */
    public static void main(String[] args) throws InterruptedException, IOException {
        // 设置自定义的 SecurityManager
        System.setSecurityManager(new DefaultSecurityManager());
        // 测试是否生效
        System.out.println("测试 SecurityManager 生效");

        String userDir = System.getProperty("user.dir");
        String filePath = userDir + File.separator + "src/main/resources/木马程序.sh";
        Process permissionProcess = Runtime.getRuntime().exec("chmod +x " + filePath);
        Process process = Runtime.getRuntime().exec(filePath);
        process.waitFor();
        // 分批获取进程的正常输出
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        // 逐行读取
        String compileOutputLine;
        while ((compileOutputLine = bufferedReader.readLine()) != null) {
            System.out.println(compileOutputLine);
        }
        System.out.println("执行异常程序成功");
    }
}
