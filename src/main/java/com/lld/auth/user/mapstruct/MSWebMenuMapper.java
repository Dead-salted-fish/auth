package com.lld.auth.user.mapstruct;

import com.lld.auth.user.entity.MenuMeta;
import com.lld.auth.user.entity.SysMenu;
import com.lld.auth.user.entity.WebMenu;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface MSWebMenuMapper {
    // 使用工厂方法获取对象实例
    MSWebMenuMapper INSTANCE = Mappers.getMapper(MSWebMenuMapper.class);

    List<WebMenu> toWebMenuList(List<SysMenu>  sysMenus);

    WebMenu toWebMenu(SysMenu sysMenu);

    MenuMeta toMenuMeta(SysMenu sysMenu);


}
