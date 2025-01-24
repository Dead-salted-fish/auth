package com.lld.auth.user.controller;

import com.lld.auth.user.entity.DTO.SysUserDto;
import com.lld.auth.user.entity.SysUser;
import com.lld.auth.user.entity.VO.SysUserVo;
import com.lld.auth.user.service.SysUserService;
import com.lld.saltedfishutils.utils.ReturnResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
@RequestMapping("/auth")
@RestController
public class UserController {
    @Autowired
    private SysUserService sysUserService;

    @PostMapping("/user/register")
    public ReturnResult registerUser(@RequestBody SysUser sysUser) {
        return  sysUserService.registerUser(sysUser);

    }

    @PostMapping("/user/getClientRsaPublicKey")
    public ReturnResult getClientRsaPublicKey() throws Exception {
        return  sysUserService.getClientRsaPublicKey();

    }

    @GetMapping("/user/getMenus")
    public ReturnResult getClientRsaPublicKey(String token) throws Exception {
        System.out.println("token"+ token);
        return  sysUserService.getMenus();

    }
    @GetMapping("/user/getUserList")
    public ReturnResult getUserList() throws Exception {

        return  sysUserService.getUserList();

    }

    @GetMapping("/user/getUserById")
    public ReturnResult getUserById(Long id) throws Exception {

        return  sysUserService.getUserById(id);

    }

    @PostMapping("/user/add")
    public ReturnResult addUser(@RequestBody SysUserDto sysUserDto) throws Exception {

        return  sysUserService.addUser(sysUserDto);

    }

    @PostMapping("/user/update")
    public ReturnResult updateUser(@RequestBody SysUserDto sysUserDto) throws Exception {

        return  sysUserService.updateUser(sysUserDto);

    }

    @PostMapping("/user/deleteById")
    public ReturnResult deleteById(@RequestBody SysUserDto sysUserDto) throws Exception {

        return  sysUserService.deleteById(sysUserDto);

    }

    @GetMapping("/dictionary/getUserRolesTree")
    public ReturnResult getUserRolesTree() throws Exception {

        return  sysUserService.getUserRolesTree();

    }
}
