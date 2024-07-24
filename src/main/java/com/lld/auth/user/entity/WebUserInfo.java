package com.lld.auth.user.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class WebUserInfo {

    private Long id;

    private String userName;


    private String encoderType;

    private String avatar;

    private String email;

    private String phoneNumber;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone="GMT+8")
    private Date loginDate;

    private String status;

    private String token;

}
