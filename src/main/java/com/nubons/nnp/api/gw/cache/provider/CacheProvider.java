package com.nubons.nnp.api.gw.cache.provider;

public interface CacheProvider {
    void put(String key, Object value);
    void put(String key, Object value, long ttl);
    Object get(String key);
    <T> T get(String key, Class<T> cls);
    void evict(String key);
}
