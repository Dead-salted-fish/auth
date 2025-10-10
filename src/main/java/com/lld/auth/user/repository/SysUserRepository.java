package com.lld.auth.user.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lld.auth.user.entity.SysUser;
import com.lld.auth.user.mapper.SysUserMapper;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
public class SysUserRepository {
    private SysUserMapper sysUserMapper;
    public SysUserRepository(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }


    public void insertSingle(SysUser sysUser) {
        sysUserMapper.insertSingle(sysUser);
    }

    public void updateLoginDate(Long id, Date loginDate) {
        sysUserMapper.updateLoginDate(id, loginDate);
    }

    public List<SysUser> selectList(SysUser sysUser) {
        return sysUserMapper.selectList(null);
    }

    public SysUser selectById(Long id) {
        return sysUserMapper.selectById(id);
    }

    public int deleteById(Long  id) {
        QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id",id);
        return sysUserMapper.delete(queryWrapper);
    }

    public int getCountUserName(String username) {
        return sysUserMapper.getCountUserName(username);
    }
}
