package com.lld.auth.user.mapper;

import com.lld.auth.user.entity.EncryptedRecords;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
* @author saltedFish
* @description 针对表【encrypted_records】的数据库操作Mapper
* @createDate 2024-06-04 19:12:30
* @Entity com.lld.auth.user.entity.EncryptedRecords
*/
@Mapper
public interface EncryptedRecordsMapper extends BaseMapper<EncryptedRecords> {
   int  addEncryptedRecord(@Param("encryptedRecords") EncryptedRecords encryptedRecords);

    EncryptedRecords selectLatestEncryptedRecord();
}




