package com.lld.auth.security.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 *  登录限制
 *  lua脚本执行后的返回结果，用以确定限制时间内是否登录失败次数过多从而限制登录
 * */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginLimit {
    private String status ;//状态
    private Long lockTtl ;//锁定时间
    private Integer remainingAttempts ;//剩余尝试次数
    private String action;//动作
}
