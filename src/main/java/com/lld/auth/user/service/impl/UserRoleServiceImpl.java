package com.lld.auth.user.service.impl;


import com.lld.auth.security.entity.MyUsernamePasswordAuthenticationToken;
import com.lld.auth.user.entity.UserRole;
import com.lld.auth.user.mapper.UserRoleMapper;
import com.lld.auth.user.repository.UserRoleRepository;
import com.lld.auth.user.service.IUserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * <p>
 * 用户-角色 表 服务实现类
 * </p>
 *
 * @author saltedFish
 * @since 2026-10-02
 */
@Service
public class UserRoleServiceImpl  implements IUserRoleService {

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Override
    @Transactional
    public void updateUserRoles(Long id, String roles, Date now) {

        // 清除旧的角色关联
        userRoleRepository.deleteByUserId(id);

        // 添加新的角色关联
        List<UserRole> newRoles = new ArrayList<>();
        LocalDateTime nowLocal = now.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        MyUsernamePasswordAuthenticationToken authentication = (MyUsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        Long userId = authentication.getUserId();
        if (StringUtils.hasText(roles)) {
            String[] roleArray = roles.split(",");
            for (String role : roleArray) {
                UserRole userRole = new UserRole();
                userRole.setUserId(id);
                userRole.setRoleId(Long.parseLong(role));
                userRole.setCreateTime(nowLocal);
                userRole.setUpdateTime(nowLocal);
                userRole.setCreater(userId);
                userRole.setUpdater(userId);
                newRoles.add(userRole);
            }
        }
        userRoleRepository.saveAll(newRoles);
    }

    @Override
    public void deleteUserRolesByUserId(Long userId) {
        userRoleRepository.deleteByUserId(userId);
    }

    @Override
    public List<UserRole> getUserRolesByUserIds(List<Long> userIds) {
        if(CollectionUtils.isEmpty(userIds)){
            return new ArrayList<>();
        }
        return userRoleRepository.getUserRolesByUserIds(userIds);
    }
}
