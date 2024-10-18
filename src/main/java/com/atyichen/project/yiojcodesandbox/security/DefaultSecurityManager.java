package com.atyichen.project.yiojcodesandbox.security;

import java.security.Permission;

public class DefaultSecurityManager extends SecurityManager{
    @Override
    public void checkPermission(Permission perm) {
        System.out.println("默认放行");
//        super.checkPermission(perm);
    }
}
