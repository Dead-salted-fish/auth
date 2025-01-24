package com.lld.auth.user.entity.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class SysMenuDto {
    /**
     * 菜单主键ID
     */
    private Long id;

    /**
     * 菜单名称
     */
    private String title;

    /**
     * 菜单图标
     */
    private String icon;

    /**
     * 父菜单ID
     */
    private Long parentId;

    /**
     * 显示顺序
     */
    private Integer orderNo;

    /**
     * 路由地址
     */
    private String path;

    /**
     * 组件路径
     */
    private String component;

    /**
     * 菜单类型
     */
    private String menuType;

    /**
     * 权限标识
     */
    private String perms;

    /**
     * 状态 (0 禁用 1启用)
     */
    private String status;

    /**
     * 创建时间
     */
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    private Date createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    private Date updateTime;

    /**
     * 创建人
     */
    private Long creater;

    /**
     * 更新人
     */
    private Long updater;

    /**
     * 备注
     */
    private String remark;

}
