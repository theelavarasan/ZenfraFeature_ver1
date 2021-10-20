package com.zenfra.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@Configuration
public class RedisUtil<T> {
	
	@Autowired
    private RedisTemplate<String,T> redisTemplate;
    private HashOperations<String,Object,T> hashOperation;
    private ValueOperations<String,T> valueOperations;
    @Autowired
    RedisUtil(RedisTemplate<String,T> redisTemplate){
        this.redisTemplate = redisTemplate;
        this.hashOperation = redisTemplate.opsForHash();
        redisTemplate.opsForList();
        this.valueOperations = redisTemplate.opsForValue();
     }
     public void putMap(String redisKey,Object key,T data) {
        hashOperation.put(redisKey, key, data);
     }
     public T getMapAsSingleEntry(String redisKey,Object key) {
        return  hashOperation.get(redisKey,key);
     }
     public Map<Object, T> getMapAsAll(String redisKey) {
        return hashOperation.entries(redisKey);
     }
     public void putValue(String key,T value) {
        valueOperations.set(key, value);
     }
     public void putValueWithExpireTime(String key,T value,long timeout,TimeUnit unit) {
        valueOperations.set(key, value, timeout, unit);
     }
     public T getValue(String key) {
        return valueOperations.get(key);
     }
     public void setExpire(String key,long timeout,TimeUnit unit) {
       redisTemplate.expire(key, timeout, unit);
     }
     
     public List<String> getKeys(String keyPattern) {
 		List<String> keyList = new ArrayList<String>();
 		Set<String> keySet = redisTemplate.keys(keyPattern + "*");
 		if(!keySet.isEmpty()) {
 			keyList.addAll(keySet);
 		}
 		return keyList;
 	}
     
     public void slaveOf() {
    	 redisTemplate.slaveOfNoOne();
     }
     
     public List<String> getReportKeys() {
  		List<String> keyList = new ArrayList<String>();
  		Set<String> keySet = redisTemplate.keys("REPORT*");
  		System.out.println("!!!!! keySet: " + keySet);
  		
  		if(!keySet.isEmpty()) {
  			keyList.addAll(keySet);
  		}
  		return keyList;
  	}

    public List<String> getDFKeys() {
        List<String> keyList = new ArrayList<String>();
        Set<String> keySet = redisTemplate.keys("DF_*");
        System.out.println("!!!!! keySet: " + keySet);

        if(!keySet.isEmpty()) {
            keyList.addAll(keySet);
        }
        return keyList;
    }
}