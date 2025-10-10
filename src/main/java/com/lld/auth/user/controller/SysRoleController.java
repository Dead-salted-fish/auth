package com.lld.auth.user.controller;

import com.lld.auth.annotation.RequireRole;
import com.lld.auth.user.entity.DTO.SysRoleDto;
import com.lld.auth.user.service.SysRoleService;
import com.lld.saltedfishutils.utils.ReturnResult;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/auth/role")
@RestController
@RequireRole(roles = {"saltedadmin","admin"})
public class SysRoleController {

    private SysRoleService sysRoleService;
    public SysRoleController(SysRoleService sysRoleService) {
        this.sysRoleService = sysRoleService;
    }

    @GetMapping("/getRolesList")
    public ReturnResult getRolesList() {
        return sysRoleService.getRolesList();

    }

    @PostMapping("/addRole")
    public ReturnResult addRole(@RequestBody SysRoleDto sysRoleDto) {
        return sysRoleService.addRole(sysRoleDto);

    }

    @PostMapping("/deleteRole")
    public ReturnResult deleteRole(@RequestBody SysRoleDto sysRoleDto) {
        return sysRoleService.deleteRole(sysRoleDto);

    }

    @PostMapping("/updateRole")
    public ReturnResult updateRole(@RequestBody SysRoleDto sysRoleDto) {
        return sysRoleService.updateRole(sysRoleDto);

    }


    @GetMapping("/getRoleById")
    public ReturnResult selectById( SysRoleDto sysRoleDto) {
        return sysRoleService.getRoleById(sysRoleDto);

    }

    @GetMapping("/getRoleSelectOptions")
    public ReturnResult getRoleSelectOptions() {
        return sysRoleService.getRoleSelectOptions();

    }
     /**
      * 根据id获取角色详情
      * */
    @GetMapping("/getRoleDetailById")
    public ReturnResult getRoleDetailById(Long roleId) {
        return sysRoleService.getRoleDetailById(roleId);

    }


}
