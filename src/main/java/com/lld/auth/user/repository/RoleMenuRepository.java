package com.lld.auth.user.repository;

import com.lld.auth.user.entity.RoleMenu;
import com.lld.auth.user.mapper.RoleMenuMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RoleMenuRepository {
    private RoleMenuMapper roleMenuMapper;
    public RoleMenuRepository(RoleMenuMapper roleMenuMapper) {
        this.roleMenuMapper = roleMenuMapper;
    }

    public int deleteByRoleId(Long roleId) {
        return roleMenuMapper.deleteByRoleId(roleId);

    }

    public int insertRoleMenus(List<RoleMenu> roleMenus) {
        return roleMenuMapper.insertRoleMenus(roleMenus);
    }

    public List<RoleMenu> selectByRoleId(Long roleId) {
        return roleMenuMapper.selectByRoleId(roleId);
    }
}
