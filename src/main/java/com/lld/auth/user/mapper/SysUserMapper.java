package com.lld.auth.user.mapper;

import com.lld.auth.user.entity.SysUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

/**
* @author saltedFish
* @description 针对表【sys_user】的数据库操作Mapper
* @createDate 2024-03-20 15:45:56
* @Entity com.lld.auth.user.entity.SysUser
*/
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    SysUser getByUserName(@Param("userName") String userName);

    int getCountUserName(@Param("userName") String userName);

    void insertSingle(@Param("sysUser") SysUser sysUser);

    void updateLoginDate(@Param("id")Long id, @Param("loginDate")Date loginDate);
}




