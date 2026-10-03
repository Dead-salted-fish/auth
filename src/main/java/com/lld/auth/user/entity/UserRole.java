package com.lld.auth.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户-角色 表
 * </p>
 *
 * @author saltedFish
 * @since 2026-10-02
 */
@TableName("user_role")
@Data
public class UserRole implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 菜单主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 角色id
     */
    private Long roleId;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    private Long creater;

    /**
     * 更新人
     */
    private Long updater;


    @Override
    public String toString() {
        return "UserRole{" +
            "id = " + id +
            ", roleId = " + roleId +
            ", userId = " + userId +
            ", createTime = " + createTime +
            ", updateTime = " + updateTime +
            ", creater = " + creater +
            ", updater = " + updater +
            "}";
    }
}
