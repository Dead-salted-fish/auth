package com.lld.auth.user.controller;

import com.lld.auth.annotation.RequireRole;
import com.lld.auth.user.entity.DTO.RoleMenuAssignmentDto;
import com.lld.auth.user.service.RoleMenuService;
import com.lld.saltedfishutils.web.result.ReturnResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/auth/roleMenu")
@RestController
@PreAuthorize("hasAnyRole('saltedadmin','admin')")
public class RoleMenuController {

    @Autowired
    private RoleMenuService sysMenuService;
    @PostMapping("/updateRoleMenu")
    public ReturnResult updateRoleMenu(@RequestBody RoleMenuAssignmentDto roleMenuAssignmentDto) {
        return sysMenuService.updateRoleMenu(roleMenuAssignmentDto);

    }

    /**
     *
     * */
    @GetMapping("/getMenuIdsByRoleId")
    public ReturnResult getMenuIdsByRoleId(@RequestParam("roleId") Long roleId) {
        return sysMenuService.getRoleMenuIdsGroupedByType(roleId);
    }

    @GetMapping("/getRoleOwnedMenuTreeByRoleId")
    public ReturnResult getRoleOwnedMenuTreeByRoleId( Long roleId) {
        return sysMenuService.getRoleOwnedMenuTreeByRoleId(roleId);
    }



}
