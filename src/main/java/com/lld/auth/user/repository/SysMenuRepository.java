package com.lld.auth.user.repository;

import com.lld.auth.user.entity.SysMenu;
import com.lld.auth.user.mapper.SysMenuMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SysMenuRepository {

    private SysMenuMapper sysMenuMapper;
    public SysMenuRepository(SysMenuMapper sysMenuMapper) {
        this.sysMenuMapper = sysMenuMapper;
    }

    public List<SysMenu> selectList(Object o) {
        return sysMenuMapper.selectList(null);
    }

    public int insert(SysMenu sysMenu) {
       return sysMenuMapper.insert(sysMenu);
    }

    public SysMenu selectById(Long id) {
        return sysMenuMapper.selectById(id);
    }

    public int updateMenuById(SysMenu sysMenu) {
        return sysMenuMapper.updateMenuById(sysMenu);
    }

    public int deleteById(Long id) {
        return sysMenuMapper.deleteById(id);
    }

    public List<SysMenu> selectByIds(List<Long> menuIds) {
        return sysMenuMapper.selectBatchIds(menuIds);
    }
}
