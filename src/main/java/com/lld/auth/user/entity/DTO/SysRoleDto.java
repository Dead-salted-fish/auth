package com.lld.auth.user.entity.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class SysRoleDto {
    /**
     * 菜单主键ID
     */

    private Long id;

    /**
     * 角色
     */

    private String role;

    /**
     * 角色名称
     */

    private String roleName;

    /**
     * 显示顺序
     */

    private Long orderNo;

    /**
     * 状态 0 禁用 1启用
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