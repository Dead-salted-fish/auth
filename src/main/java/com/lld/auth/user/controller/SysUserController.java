package com.lld.auth.user.controller;

import com.lld.auth.annotation.RequireRole;
import com.lld.auth.user.entity.DTO.SysUserDto;
import com.lld.auth.user.entity.SysUser;
import com.lld.auth.user.service.SysUserService;
import com.lld.saltedfishutils.web.result.ReturnResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
@RequestMapping("/auth")
@RestController
@RequireRole(roles = {"saltedadmin","admin"})
public class SysUserController {
    @Autowired
    private SysUserService sysUserService;

    @PostMapping("/user/register")
    public ReturnResult registerUser(@RequestBody SysUser sysUser) {
        return  sysUserService.registerUser(sysUser);

    }
    @RequireRole(roles = {"noAuthentication"})
    @PostMapping("/user/getClientRsaPublicKey")
    public ReturnResult getClientRsaPublicKey() throws Exception {
        return  sysUserService.getClientRsaPublicKey();

    }

    @GetMapping("/user/getMenus")
    @RequireRole(roles = {"noAuthentication"})
    public ReturnResult getMenus(String token) throws Exception {
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
  /**
   * 根据用户id获取用户详情
   **/
    @GetMapping("/user/getUserDetailById")
    public ReturnResult getUserDetailById(Long  id) throws Exception {

        return  sysUserService.getUserDetailById(id);

    }

    /**
     * 用户心跳，更新用户状态
     * roles = {"noAuthentication"} 表示不需要权限，但还是要登录的
     * */
    @RequireRole(roles = {"noAuthentication"})
    @GetMapping("/user/userHeartBeat")
    public ReturnResult userHeartBeat(Long  id,Integer status) throws Exception {

        return  sysUserService.userHeartBeat(id,status);

    }

    /**
     *  获取用户在线数量
     * **/
    @GetMapping("/user/getOnlineStatistics")
    public ReturnResult getOnlineStatistics() throws Exception {

        return  sysUserService.getOnlineStatistics();

    }

}
