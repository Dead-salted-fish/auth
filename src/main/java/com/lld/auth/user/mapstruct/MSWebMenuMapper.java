package com.lld.auth.user.mapstruct;
import com.lld.auth.user.entity.*;
import com.lld.auth.user.entity.DTO.SysMenuDto;
import com.lld.auth.user.entity.VO.SysMenuVo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface MSWebMenuMapper {
    // 使用工厂方法获取对象实例
    MSWebMenuMapper INSTANCE = Mappers.getMapper(MSWebMenuMapper.class);

    List<WebMenu> toWebMenuList(List<SysMenu>  sysMenus);

    WebMenu toWebMenu(SysMenu sysMenu);

    MenuMeta toMenuMeta(SysMenu sysMenu);

    SysMenuVo sysMenuToSysMenuVo(SysMenu sysMenu);


    SysMenu sysMenuDtoToSysMenu(SysMenuDto sysMenuDto);

    @Named("getParentId")
    default Long getParentId(List<Long> parentIdList) {
        if (parentIdList != null && !parentIdList.isEmpty()) {
            return parentIdList.get(parentIdList.size()-1);
        }
        return null;
    }

    SysMenu SysMenuDtoToSysMenu(SysMenuDto sysMenuDto);
}
