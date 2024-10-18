package com.atyichen.project.yiojcodesandbox;


import com.atyichen.project.yiojcodesandbox.model.ExecuteCodeRequest;
import com.atyichen.project.yiojcodesandbox.model.ExecuteCodeResponse;

public interface CodeSandBox {
    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);
}
