package com.lld.auth.user.entity.VO;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
@Data
public class SysUserVo {

    /**
     * 用户ID
     */

    private Long id;

    /**
     * 用户名
     */

    private String userName;

    private String passWord;

    /**
     * 角色
     */

    private String roles;

    private String rolesStr;

    /**
     * 用户头像
     */

    private String avatar;

    /**
     * 用户邮箱
     */

    private String email;

    /**
     * 手机号码
     */
    private String phoneNumber;

    /**
     * 最后登录时间
     */
    @JsonFormat(pattern="yyyy-MM-dd",timezone="GMT+8")

    private Date loginDate;

    /**
     * 帐号状态（1正常 0停用）
     */

    private String status;

    /**
     * 帐号状态（1正常 0停用）
     */

    private String statusStr;

    /**
     * 创建人
     */
    private Long creater;

    /**
     * 更新人
     */
    private Long updater;

    /**
     * 创建时间
     */

    @JsonFormat(pattern="yyyy-MM-dd",timezone="GMT+8")
    private Date createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern="yyyy-MM-dd",timezone="GMT+8")

    private Date updateTime;

    /**
     * 备注
     */

    private String remark;


}
