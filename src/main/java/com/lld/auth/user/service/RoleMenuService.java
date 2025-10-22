package com.lld.auth.user.service;

import com.lld.auth.user.entity.DTO.RoleMenuAssignmentDto;
import com.lld.auth.user.entity.RoleMenu;
import com.lld.saltedfishutils.web.result.ReturnResult;

import java.util.List;

/**
* @author saltedFish
* @description 针对表【role_menu】的数据库操作Service
* @createDate 2025-07-02 22:06:29
*/
public interface RoleMenuService {

    ReturnResult updateRoleMenu(RoleMenuAssignmentDto roleMenuAssignmentDto);

    List<RoleMenu> getMenuIdsByRoleId(Long roleId);

    ReturnResult getRoleMenuIdsGroupedByType(Long roleId);

    ReturnResult getRoleOwnedMenuTreeByRoleId(Long roleId);

    void deleteByRoleId(Long roleId);
}
