package com.ksyun.course;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

@EnableAsync
@Component
public class RedisTest {


    public void redisTest() {
        RedisURI redisUri = RedisURI.builder()
                .withHost("localhost")  // 设置主机
                .withPort(6379)  // 设置端口号
                .withDatabase(8)  // 设置默认数据库
                .build();
        RedisClient redisClient = RedisClient.create(redisUri);

        StatefulRedisConnection<String, String> connection = redisClient.connect();     // <3> 创建线程安全的连接
        RedisCommands<String, String> redisCommands = connection.sync();
        redisCommands.ping();
        System.out.println("Connected to Redis!");
        redisCommands.sadd("test","okkk hello");
//        System.out.println(redisTemplate.opsForHash().entries("user-money"));
        System.out.println(redisCommands.hgetall("user-money"));
        System.out.println(redisCommands.smembers("test"));

    }
}
