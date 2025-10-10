package com.lld.auth.user.mapstruct;

import com.lld.auth.user.entity.DTO.SysMenuDto;
import com.lld.auth.user.entity.SysMenu;
import com.lld.auth.user.entity.VO.SysMenuVo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MSSysMenuMapper {
    // 使用工厂方法获取对象实例
    MSSysMenuMapper INSTANCE = Mappers.getMapper(MSSysMenuMapper.class);



    SysMenuVo sysMenuToSysMenuVo(SysMenu sysMenu);


    SysMenu sysMenuDtoToSysMenu(SysMenuDto sysMenuDto);


    SysMenu SysMenuDtoToSysMenu(SysMenuDto sysMenuDto);
}
