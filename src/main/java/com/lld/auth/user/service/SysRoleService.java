package com.lld.auth.user.service;

import com.lld.auth.user.entity.DTO.SysRoleDto;
import com.lld.saltedfishutils.web.result.ReturnResult;

import java.util.List;
import java.util.Map;

/**
* @author saltedFish
* @description 针对表【sys_role】的数据库操作Service
* @createDate 2025-05-28 14:04:27
*/
public interface SysRoleService  {

    ReturnResult getRolesList();

    ReturnResult addRole(SysRoleDto sysRoleDto);

    ReturnResult getRoleById(SysRoleDto sysRoleDto);

    ReturnResult updateRole(SysRoleDto sysRoleDto);

    ReturnResult deleteRole(SysRoleDto sysRoleDto);

    ReturnResult getRoleSelectOptions();

    List<String> getRolesById(List<Long> roleIds);

    Map<Long, String> getRoleMap();

    ReturnResult getRoleDetailById(Long roleId);
}
