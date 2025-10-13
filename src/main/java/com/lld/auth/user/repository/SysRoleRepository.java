package com.lld.auth.user.repository;

import com.lld.auth.user.entity.SysRole;
import com.lld.auth.user.mapper.SysRoleMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SysRoleRepository {
    private SysRoleMapper sysRoleMapper;
    public SysRoleRepository(SysRoleMapper sysRoleMapper) {
        this.sysRoleMapper = sysRoleMapper;
    }

    public List<SysRole> selectList() {
        return sysRoleMapper.selectList(null);
    }

    public int addRole(SysRole sysRole) {
        return sysRoleMapper.insert(sysRole);
    }

    public SysRole getRoleById(Long id) {
       return sysRoleMapper.selectById(id);
    }

    public int updateRole(SysRole sysRole) {
        return sysRoleMapper.updateById(sysRole);
    }

    public SysRole selectById(Long id) {
     return  sysRoleMapper.selectById(id);
    }



    public int deleteById(Long id) {
        return sysRoleMapper.deleteById(id);
    }

    public List<SysRole> selectByIds(List<Long> roleIds) {
       return sysRoleMapper.selectByIds(roleIds);

    }
}
