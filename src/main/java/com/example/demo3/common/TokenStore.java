package com.example.demo3.common;

import com.example.demo3.entity.SysUser;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 登录 Token 统一存储组件
 *
 * 设计目标：Redis 是首选存储，但 Redis 未安装/故障时不能阻断业务。
 *   - save：Redis 可用 → 写 Redis（2小时 TTL）；Redis 异常 → 降级内存 Map
 *   - get  ：先查 Redis，异常或未命中 → 查内存 Map（命中即顺延过期时间）
 *   - delete：Redis 与内存 Map 双删，保证注销立即生效
 *
 * 内存兜底为进程内有效（重启后登录态清空，需重新登录），仅用于开发/降级场景。
 */
@Slf4j
@Component
public class TokenStore {

    /** 登录 Token 在 Redis 的键：auth:token:{token} → SysUser */
    public static final String TOKEN_KEY_PREFIX = "auth:token:";
    private static final long TOKEN_TTL_HOURS = 2;
    private static final long TOKEN_TTL_MILLIS = TOKEN_TTL_HOURS * 3600 * 1000;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /** 内存兜底存储：token → 用户 + 过期时间戳 */
    private final Map<String, MemoryEntry> memoryStore = new ConcurrentHashMap<>();

    private record MemoryEntry(SysUser user, long expireAt) {}

    /**
     * 保存登录态（Redis 优先，失败降级内存）
     */
    public void save(String token, SysUser user) {
        String key = TOKEN_KEY_PREFIX + token;
        try {
            redisTemplate.opsForValue().set(key, user, TOKEN_TTL_HOURS, TimeUnit.HOURS);
            log.debug("[TokenStore] 已写入 Redis：{}", key);
        } catch (Exception e) {
            log.warn("[TokenStore] Redis 不可用，降级内存存储（重启后需重新登录）：{}", e.getMessage());
            memoryStore.put(token, new MemoryEntry(user, System.currentTimeMillis() + TOKEN_TTL_MILLIS));
        }
    }

    /**
     * 查询登录态；命中内存条目时自动顺延过期时间（滑动过期）
     * @return 用户对象；不存在或已过期返回 null
     */
    public SysUser get(String token) {
        // 1. 先查 Redis
        try {
            Object cached = redisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + token);
            if (cached instanceof SysUser user) {
                return user;
            }
        } catch (Exception e) {
            log.warn("[TokenStore] Redis 读取异常，转查内存兜底：{}", e.getMessage());
        }

        // 2. 查内存兜底
        MemoryEntry entry = memoryStore.get(token);
        if (entry == null) {
            return null;
        }
        if (System.currentTimeMillis() > entry.expireAt()) {
            memoryStore.remove(token);
            return null;
        }
        // 滑动过期：活跃用户续期
        memoryStore.put(token, new MemoryEntry(entry.user(), System.currentTimeMillis() + TOKEN_TTL_MILLIS));
        return entry.user();
    }

    /**
     * 删除登录态（注销）：Redis 与内存双删
     */
    public void delete(String token) {
        try {
            redisTemplate.delete(TOKEN_KEY_PREFIX + token);
        } catch (Exception e) {
            log.warn("[TokenStore] Redis 删除异常（已忽略，内存兜底不受影响）：{}", e.getMessage());
        }
        memoryStore.remove(token);
    }
}
