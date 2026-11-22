package com.lld.auth.user.entity.VO;

import lombok.Data;

/**
 * 只暴露 keyId 与公钥。
 * 字段名与 EncryptedRecords 一致，前端契约不变；私钥及数据密钥一律不出现在接口层。
 */
@Data
public class ClientRsaPublicKeyVO {
    /** 前端登录时作为 clientRsa 参数回传 */
    private Long id;
    private String clientRsaPublickey;
}
