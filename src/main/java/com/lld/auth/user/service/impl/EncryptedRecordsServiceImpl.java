package com.lld.auth.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lld.auth.user.entity.EncryptedRecords;
import com.lld.auth.user.service.EncryptedRecordsService;
import com.lld.auth.user.mapper.EncryptedRecordsMapper;
import org.springframework.stereotype.Service;

/**
* @author saltedFish
* @description 针对表【encrypted_records】的数据库操作Service实现
* @createDate 2024-06-04 19:12:30
*/
@Service
public class EncryptedRecordsServiceImpl extends ServiceImpl<EncryptedRecordsMapper, EncryptedRecords>
    implements EncryptedRecordsService{

}




