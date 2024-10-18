package com.atyichen.project.yiojcodesandbox.utils;

import cn.hutool.core.util.StrUtil;
import com.atyichen.project.yiojcodesandbox.model.ExecuteMessage;
import org.springframework.util.StopWatch;

import java.io.*;

public class ProcessUtils {
    public static ExecuteMessage runProcessAndGetMessage(String compileCmd, String opName) {
        ExecuteMessage executeMessage = new ExecuteMessage();
        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            // 等待进程执行完 获取错误码
            int exitValue = compileProcess.waitFor();
            executeMessage.setExitValue(exitValue);

            // 正常退出
            if (exitValue == 0) {
                System.out.println(opName + "成功");
                // 获取 编译后 输出到控制台的信息
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(compileProcess.getInputStream()));
                StringBuilder compileOutputStringBuilder = new StringBuilder();

                // 逐行读取
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    compileOutputStringBuilder.append(compileOutputLine);
                }
                executeMessage.setMessage(compileOutputStringBuilder.toString());
//                System.out.println(compileOutputStringBuilder);
            } else {
                System.out.println(opName + "失败");
                // 获取 编译后 输出到控制台的信息
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(compileProcess.getInputStream()));
                StringBuilder compileOutputStringBuilder = new StringBuilder();

                // 逐行读取
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    compileOutputStringBuilder.append(compileOutputLine);
                }

                // 获取 编译后 进程的错误输出
                BufferedReader errorBufferedReader = new BufferedReader(new InputStreamReader(compileProcess.getErrorStream()));
                StringBuilder errorCompileOutputStringBuilder = new StringBuilder();

                // 逐行读取
                String errorCompileOutputLine;
                while ((errorCompileOutputLine = errorBufferedReader.readLine()) != null) {
                    errorCompileOutputStringBuilder.append(errorCompileOutputLine);
                }

//                System.out.println(compileOutputStringBuilder);
//                System.out.println(errorCompileOutputStringBuilder);
                executeMessage.setMessage(compileOutputStringBuilder.toString());
                executeMessage.setErrorMessage(errorCompileOutputStringBuilder.toString());
            }
            stopWatch.stop();
            executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return executeMessage;
    }

    /**
     * 支持杭电oj形式的判题
     * 交互式执行进程并获取进程信息
     *
     * @param runCmd
     * @param args
     * @return
     */
    public static ExecuteMessage runProcessInterAndGetMessage(String runCmd, String args) {
        ExecuteMessage executeMessage = new ExecuteMessage();
        try {
            Process runProcess = Runtime.getRuntime().exec(runCmd);
            // 从控制台输入参数
            OutputStream outputStream = runProcess.getOutputStream();
            // 通过writer往流中写
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            String[] arguments = args.split(" ");
            String join = StrUtil.join("\n", arguments) + "\n";
            outputStreamWriter.write(join);
            // 清空
            outputStreamWriter.flush();

            // 等待进程执行完 获取错误码
            int exitValue = runProcess.waitFor();
            executeMessage.setExitValue(exitValue);

            // 正常退出
            if (exitValue == 0) {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                StringBuilder compileOutputStringBuilder = new StringBuilder();

                // 逐行读取
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    compileOutputStringBuilder.append(compileOutputLine);
                }
                executeMessage.setMessage(compileOutputStringBuilder.toString());
                System.out.println(compileOutputStringBuilder);
            } else {
                // 获取 编译后 输出到控制台的信息
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                StringBuilder compileOutputStringBuilder = new StringBuilder();

                // 逐行读取
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    compileOutputStringBuilder.append(compileOutputLine);
                }

                // 获取 编译后 进程的错误输出
                BufferedReader errorBufferedReader = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()));
                StringBuilder errorCompileOutputStringBuilder = new StringBuilder();

                // 逐行读取
                String errorCompileOutputLine;
                while ((errorCompileOutputLine = errorBufferedReader.readLine()) != null) {
                    errorCompileOutputStringBuilder.append(errorCompileOutputLine);
                }

                System.out.println(compileOutputStringBuilder);
                System.out.println(errorCompileOutputStringBuilder);
                executeMessage.setMessage(compileOutputStringBuilder.toString());
                executeMessage.setErrorMessage(errorCompileOutputStringBuilder.toString());

            }
            // 记得资源的释放！
            outputStreamWriter.close();
            outputStream.close();
            runProcess.destroy();

        } catch (Exception e) {
            e.printStackTrace();
        }


        return executeMessage;
    }
}
