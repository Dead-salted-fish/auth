package com.lld.auth.user.mapstruct;

import com.lld.auth.user.entity.DTO.SysUserDto;
import com.lld.auth.user.entity.SysUser;
import com.lld.auth.user.entity.VO.SysUserVo;
import com.lld.auth.user.entity.WebUserInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface MSUserMapper {
    // 使用工厂方法获取对象实例
    MSUserMapper INSTANCE = Mappers.getMapper(MSUserMapper.class);
    WebUserInfo toWebUserInfo(SysUser sysUser);
    @Mapping(target = "passWord", ignore = true)
    SysUserVo SysUserToSysUserVo(SysUser sysUser);

    List<SysUserVo> SysUserListToSysUserVoList(List<SysUser> sysUserList);

    SysUser SysUserDtoToSysUser(SysUserDto sysUserDto);
}
