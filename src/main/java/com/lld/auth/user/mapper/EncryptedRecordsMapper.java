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

    int addEncryptedRecord(@Param("encryptedRecords") EncryptedRecords encryptedRecords);

    /** 只返回唯一的 active(type=0,status=1)，ORDER BY + LIMIT 1 兜底 */
    EncryptedRecords selectLatestEncryptedRecord();

    /** 轮换：把当前 active 置为历史(status=0)，不删除、不改动私钥列 */
    int retireActiveRsaKey();

    /** 应急：标记某把密钥已泄露(status=3) */
    int markCompromised(@Param("id") Long id);
}




