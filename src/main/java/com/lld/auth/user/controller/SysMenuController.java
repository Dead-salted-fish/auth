package com.lld.auth.user.controller;

import com.lld.auth.annotation.RequireRole;
import com.lld.auth.user.entity.DTO.SysMenuDto;
import com.lld.auth.user.service.SysMenuService;
import com.lld.saltedfishutils.utils.ReturnResult;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/auth/menu")
@RestController
@RequireRole(roles = {"saltedadmin","admin"})
public class SysMenuController {
    private SysMenuService sysMenuService;
    public SysMenuController(SysMenuService sysMenuService) {
        this.sysMenuService = sysMenuService;
    }

    /**
     *  获取所有菜单
     * */
    @GetMapping("/getMenuList")
    public ReturnResult list() {
        return sysMenuService.getMenulist();

    }

    /**
     *  获取父菜单树结构
     * */
    @GetMapping("/getParentTree")
    public ReturnResult getParentTree() {
        return sysMenuService.getParentTree();

    }

    @GetMapping("/getMenuTree")
    public ReturnResult getMenuTree() {
        return sysMenuService.getMenuTree();

    }

    @PostMapping("/add")
    public ReturnResult addMenu(@RequestBody SysMenuDto sysMenuDto) {
        return sysMenuService.addMenu(sysMenuDto);

    }

    @PostMapping("/update")
    public ReturnResult updateMenu(@RequestBody SysMenuDto sysMenuDto) {
        return sysMenuService.updateMenu(sysMenuDto);

    }


    @GetMapping("/getMenuById")
    public ReturnResult getMenuById(Long id) {
        return sysMenuService.getMenuById(id);
    }

    @PostMapping("/deleteMenuById")
    public ReturnResult deleteMenuById(@RequestBody SysMenuDto sysMenuDto) {
        return sysMenuService.deleteMenuById(sysMenuDto);
    }

    /**
     * 根据id获取菜单详情
     * */
    @GetMapping("/getMenuDetailById")
    public ReturnResult getMenuDetailById(Long id) {
        return sysMenuService.getMenuDetailById(id);
    }

}
