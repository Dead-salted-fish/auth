package com.lld.auth.user.service;

import com.lld.auth.user.entity.SysUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lld.saltedfishutils.utils.ReturnResult;

import java.util.Date;

/**
* @author saltedFish
* @description 针对表【sys_user】的数据库操作Service
* @createDate 2024-03-20 15:45:56
*/
public interface SysUserService extends IService<SysUser> {

    SysUser getUserByUserName(String username);

    ReturnResult registerUser(SysUser sysUser);

    void updateLoginDate(Long id, Date loginDate);

    ReturnResult getClientRsaPublicKey() throws Exception;
}
