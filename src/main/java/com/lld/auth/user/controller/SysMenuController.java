package com.lld.auth.user.controller;

import com.lld.auth.user.entity.DTO.SysMenuDto;
import com.lld.auth.user.service.SysMenuService;
import com.lld.auth.user.service.SysUserService;
import com.lld.saltedfishutils.utils.ReturnResult;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/auth/menu")
@RestController
public class SysMenuController {
    private SysMenuService sysMenuService;
    public SysMenuController(SysMenuService sysMenuService) {
        this.sysMenuService = sysMenuService;
    }

    @GetMapping("/getMenuList")
    public ReturnResult list() {
        return sysMenuService.getMenulist();

    }

    @GetMapping("/getParentTree")
    public ReturnResult getParentTree() {
        return sysMenuService.getParentTree();

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

}
