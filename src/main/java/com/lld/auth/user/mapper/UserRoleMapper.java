package com.lld.auth.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lld.auth.user.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 用户-角色 表 Mapper 接口
 * </p>
 *
 * @author saltedFish
 * @since 2026-10-02
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

    void deleteByUserId(@Param("userId") Long id);

    List<UserRole> getUserRolesByUserIds(List<Long> userIds);
}
