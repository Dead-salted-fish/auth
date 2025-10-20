package com.lld.auth.user.service;

import com.lld.auth.user.entity.DTO.SysUserDto;
import com.lld.auth.user.entity.SysUser;
import com.lld.saltedfishutils.web.result.ReturnResult;

import java.util.Date;
import java.util.List;

/**
* @author saltedFish
* @description 针对表【sys_user】的数据库操作Service
* @createDate 2024-03-20 15:45:56
*/
public interface SysUserService  {

    SysUser getUserByUserName(String username);

    ReturnResult registerUser(SysUser sysUser);

    void updateLoginDate(Long id, Date loginDate);

    ReturnResult getClientRsaPublicKey() throws Exception;

    ReturnResult getMenus();

    ReturnResult getUserList();

    ReturnResult getUserRolesTree();

    ReturnResult addUser(SysUserDto sysUserDto);

    ReturnResult updateUser(SysUserDto sysUserDto);

    ReturnResult getUserById(Long id);

    ReturnResult deleteById(SysUserDto sysUserDto);

    List<String> getRolesByUserId(Long userId);

    ReturnResult getUserDetailById(Long  id);

    ReturnResult userHeartBeat(Long id, Integer status);

    ReturnResult getOnlineStatistics();
}
