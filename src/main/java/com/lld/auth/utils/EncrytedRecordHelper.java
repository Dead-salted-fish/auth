package com.lld.auth.utils;

import com.lld.auth.user.entity.EncryptedRecords;
import com.lld.auth.user.mapper.EncryptedRecordsMapper;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 密钥缓存，每次验证密码时候不用访问数据库获取私钥
 * **/
@Component
public class EncrytedRecordHelper {
    private  Map<Long, EncryptedRecords> recordsMap = new ConcurrentHashMap<>();

    private EncryptedRecordsMapper encryptedRecordsMapper;
    public EncrytedRecordHelper (EncryptedRecordsMapper encryptedRecordsMapper){
        this.encryptedRecordsMapper = encryptedRecordsMapper;

    }


    public  EncryptedRecords getEncryptedRecords(Long clientRsaId) {
        EncryptedRecords encryptedRecords = recordsMap.get(clientRsaId);
        if (encryptedRecords == null){
            encryptedRecords = encryptedRecordsMapper.selectById(clientRsaId);
            recordsMap.put(clientRsaId, encryptedRecords);
        }
        return encryptedRecords;
    }
}
