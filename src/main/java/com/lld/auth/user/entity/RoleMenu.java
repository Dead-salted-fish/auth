package com.lld.auth.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName role_menu
 */
@TableName(value ="role_menu")
@Data
public class RoleMenu implements Serializable {
    /**
     * 菜单主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 角色id
     */
    @TableField(value = "role_id")
    private Long roleId;

    /**
     * 角色
     */
    @TableField(value = "role")
    private String role;

    /**
     * 菜单id
     */
    @TableField(value = "menu_id")
    private Long menuId;

    /**
     * 菜单名称
     */
    @TableField(value = "menu_title")
    private String menuTitle;

    /**
     * 菜单名称
     */
    @TableField(value = "menu_type")
    private String menuType;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    private Date updateTime;

    /**
     * 创建人
     */
    @TableField(value = "creater")
    private Long creater;

    /**
     * 更新人
     */
    @TableField(value = "updater")
    private Long updater;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}