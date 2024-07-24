package com.lld.auth.user.controller;

import com.lld.auth.user.entity.SysUser;
import com.lld.auth.user.service.SysUserService;
import com.lld.saltedfishutils.utils.ReturnResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
@RequestMapping("/jx3/auth")
@RestController
public class UserController {
    @Autowired
    private SysUserService sysUserService;

    @PostMapping("/user/register")
    @ResponseBody
    public ReturnResult registerUser(@RequestBody SysUser sysUser) {
        return  sysUserService.registerUser(sysUser);

    }

    @PostMapping("/user/getClientRsaPublicKey")
    @ResponseBody
    public ReturnResult getClientRsaPublicKey() throws Exception {
        return  sysUserService.getClientRsaPublicKey();

    }
}
