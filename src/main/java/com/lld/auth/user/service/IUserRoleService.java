package com.lld.auth.user.service;

import com.lld.auth.user.entity.UserRole;

import java.util.Date;
import java.util.List;

/**
 * <p>
 * 用户-角色 表 服务类
 * </p>
 *
 * @author saltedFish
 * @since 2026-10-02
 */
public interface IUserRoleService {

    void updateUserRoles(Long id, String roles, Date now);

    void deleteUserRolesByUserId(Long userId);

    List<UserRole> getUserRolesByUserIds(List<Long> userIds);
}
