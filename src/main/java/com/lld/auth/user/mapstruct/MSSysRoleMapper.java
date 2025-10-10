package com.lld.auth.user.mapstruct;

import com.lld.auth.user.entity.DTO.SysRoleDto;
import com.lld.auth.user.entity.SysRole;
import com.lld.auth.user.entity.VO.SysRoleVo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface MSSysRoleMapper {
    // 使用工厂方法获取对象实例
    MSSysRoleMapper INSTANCE = Mappers.getMapper(MSSysRoleMapper.class);

    SysRoleVo sysRoleToSysRoleVo(SysRole sysRole);

    List<SysRoleVo> sysRolesListToSysRoleVoList(List<SysRole> sysRoles);

    SysRole sysRoleDtoToSysRole(SysRoleDto sysRoleDto);
}
