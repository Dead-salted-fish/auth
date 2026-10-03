package com.lld.auth.user.repository;

import com.baomidou.mybatisplus.extension.repository.CrudRepository;
import com.baomidou.mybatisplus.extension.repository.IRepository;
import com.lld.auth.user.entity.UserRole;
import com.lld.auth.user.mapper.UserRoleMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserRoleRepository extends CrudRepository<UserRoleMapper, UserRole> implements IRepository<UserRole> {
    public void deleteByUserId(Long id) {
        getBaseMapper().deleteByUserId(id);
    }

    public void saveAll(List<UserRole> newRoles) {
        getBaseMapper().insert(newRoles);
    }

    public List<UserRole> getUserRolesByUserIds(List<Long> userIds) {
        return getBaseMapper().getUserRolesByUserIds(userIds);
    }
}
