package com.lld.auth.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * 
 * @TableName sys_menu
 */
@TableName(value ="sys_menu")
@Data
public class SysMenu implements Serializable {
    /**
     * 菜单主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 菜单名称
     */
    @TableField(value = "title")
    private String title;

    /**
     * 菜单图标
     */
    @TableField(value = "icon")
    private String icon;

    /**
     * 父菜单ID
     */
    @TableField(value = "parent_id")
    private Long parentId;

    /**
     * 显示顺序
     */
    @TableField(value = "order_no")
    private Integer orderNo;

    /**
     * 路由地址
     */
    @TableField(value = "path")
    private String path;

    /**
     * 组件路径
     */
    @TableField(value = "component")
    private String component;

    /**
     * 菜单类型
     */
    @TableField(value = "menu_type")
    private String menuType;

    /**
     * 权限标识
     */
    @TableField(value = "perms")
    private String perms;

    /**
     * 状态 (0 禁用 1启用)
     */
    @TableField(value = "status")
    private String status;

    /**
     * 创建时间
     */
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    @TableField(value = "create_time")
    private Date createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
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

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}