package com.lld.auth.user.mapstruct;

import com.lld.auth.user.entity.SysUser;
import com.lld.auth.user.entity.WebUserInfo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MSUserMapper {
    // 使用工厂方法获取对象实例
    MSUserMapper INSTANCE = Mappers.getMapper(MSUserMapper.class);
    WebUserInfo toWebUserInfo(SysUser sysUser);
}
