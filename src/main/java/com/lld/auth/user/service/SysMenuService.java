package com.lld.auth.user.service;

import com.lld.auth.user.entity.DTO.SysMenuDto;
import com.lld.auth.user.entity.SysMenu;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lld.auth.user.entity.WebMenu;
import com.lld.saltedfishutils.utils.ReturnResult;

import java.util.List;

/**
* @author saltedFish
* @description 针对表【sys_menu】的数据库操作Service
* @createDate 2024-09-17 02:49:00
*/
public interface SysMenuService extends IService<SysMenu> {

    List<WebMenu> getUserMenus();

    ReturnResult getMenulist();

    ReturnResult getParentTree();

    ReturnResult addMenu(SysMenuDto sysMenuDto);

    ReturnResult getMenuById(Long id);

    ReturnResult updateMenu(SysMenuDto sysMenuDto);

    ReturnResult deleteMenuById(SysMenuDto sysMenuDto);
}
