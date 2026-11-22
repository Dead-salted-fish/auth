package com.lld.auth.utils;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.lld.auth.user.entity.EncryptedRecords;
import com.lld.auth.user.mapper.EncryptedRecordsMapper;
import com.lld.saltedfishutils.crypto.kek.KekEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * encrypted_records 表中 RSA 私钥的缓存（Caffeine）。
 *
 * 缓存的是「解密后的 PKCS#8 Base64 文本」，由
 * RSAUtil.decryptByPrivateKey(byte[], String) 直接消费，不做 PrivateKey 反序列化。
 *
 * - expireAfterAccess + maximumSize：有界、可自动失效，轮换后旧私钥不永久常驻内存
 * - loader 抛异常时不写入缓存、异常直接上抛：不存在 / 已泄露的 key 不会被缓存成 null
 *   （消除原 ConcurrentHashMap.put(id, null) 的 NPE）
 * - invalidate() 供轮换 / 应急停用显式清除
 */
@Component
public class EncrytedRecordHelper {

    private static final Logger log = LoggerFactory.getLogger(EncrytedRecordHelper.class);
    private static final long MAX_SIZE = 1000;
    private static final Duration TTL = Duration.ofMinutes(30);

    private final Cache<Long, String> privateKeyPemCache = Caffeine.newBuilder()
            .maximumSize(MAX_SIZE)
            .expireAfterAccess(TTL)
            .recordStats()
            .build();

    private final EncryptedRecordsMapper encryptedRecordsMapper;
    private final KekEnvelope kekEnvelope;

    public EncrytedRecordHelper(EncryptedRecordsMapper encryptedRecordsMapper,
                                KekEnvelope kekEnvelope) {
        this.encryptedRecordsMapper = encryptedRecordsMapper;
        this.kekEnvelope = kekEnvelope;
    }

    /** 返回 Base64(PKCS#8) 私钥文本；命中缓存时不查库、不 open、不重复加载 */
    public String getPrivateKeyPem(Long clientRsaId) {
        Long id = validateId(clientRsaId);
        return privateKeyPemCache.get(id, this::loadPrivateKeyPem);
    }

    private String loadPrivateKeyPem(Long id) {
        EncryptedRecords record = encryptedRecordsMapper.selectById(id);
        if (record == null || record.getClientRsaPrivatekey() == null
                || record.getClientRsaPrivatekey().isBlank()) {
            throw new IllegalArgumentException("密钥不存在或已失效: clientRsaId=" + id);
        }
        if (Integer.valueOf(3).equals(record.getStatus())) {
            throw new IllegalStateException("该密钥已被标记为泄露，禁止继续用于解密: clientRsaId=" + id);
        }
        // 密文列 → 明文 PKCS#8 文本；存量明文由 open() 原样返回，无需兼容分支
        return kekEnvelope.open(record.getClientRsaPrivatekey());
    }

    /**
     * 轮换
     */
    public void invalidate(Long clientRsaId) {
        if (clientRsaId != null) {
            privateKeyPemCache.invalidate(clientRsaId);
            log.info("已失效私钥缓存: clientRsaId={}, 当前缓存条目={}",
                    clientRsaId, privateKeyPemCache.estimatedSize());
        }
    }

    /** 运维巡检：命中率等，验证缓存确实生效 */
    public String cacheStats() {
        return privateKeyPemCache.stats().toString();
    }

    private Long validateId(Long clientRsaId) {
        if (clientRsaId == null || clientRsaId <= 0) {
            throw new IllegalArgumentException("非法的 clientRsaId");
        }
        return clientRsaId;
    }
}
