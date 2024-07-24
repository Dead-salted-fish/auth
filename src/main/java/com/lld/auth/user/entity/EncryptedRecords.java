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
 * @TableName encrypted_records
 */
@TableName(value ="encrypted_records")
@Data
public class EncryptedRecords implements Serializable {
    /**
     * id
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 客户端生成的rsa公钥
     */
    @TableField(value = "client_rsa_publicKey")
    private String clientRsaPublickey;

    /**
     * 客户端生成的rsa私钥
     */
    @TableField(value = "client_rsa_privateKey")
    private String clientRsaPrivatekey;

    /**
     * web生成的rsa公钥
     */
    @TableField(value = "web_rsa_publicKey")
    private String webRsaPublickey;

    /**
     * web生成的rsa私钥
     */
    @TableField(value = "web_rsa_privateKey")
    private String webRsaPrivatekey;

    /**
     * 后端ase密钥
     */
    @TableField(value = "client_aes_key")
    private String clientAesKey;

    /**
     * 前端ase密钥
     */
    @TableField(value = "web_aes_key")
    private String webAesKey;

    /**
     * 0 表示客户端 RSA私钥公钥
     * 1 表示web端 RSA私钥公钥
     * 2 表示客户端 asekey
     * 3 表示web端 asekey
     */
    @TableField(value = "type")
    private Integer type;

    /**
     * 0 表示旧的
     * 1 表示最新的
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private Date createTime;

    /**
     * 修改时间
     */
    @TableField(value = "update_time")
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}