package com.ksyun.course;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

@Service
public class UserTradeService {

    // 假设这里有一个UserService用于获取用户信息和更新用户余额的方法

    @Async
    public CompletableFuture<String> transfer(String requestId, TradeRequest tradeRequest) {
        RedisURI redisUri = RedisURI.builder()
                .withHost("localhost")  // 设置主机
                .withPort(6379)  // 设置端口号
                .withDatabase(8)  // 设置默认数据库
                .build();
        RedisClient redisClient = RedisClient.create(redisUri);
        // 连接 Redis
        try {
            StatefulRedisConnection<String, String> connection = redisClient.connect();     // <3> 创建线程安全的连接
            RedisCommands<String, String> redisCommands = connection.sync();
            redisCommands.ping();
            System.out.println("Connected to Redis!");

            boolean exists = redisCommands.sismember("tradeRequestId", requestId);
            if (exists) {
//                System.out.println("111111111");
                ApiResponse apiResponse= new ApiResponse("this request have done", 202, requestId, null);
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String json = gson.toJson(apiResponse);
                return CompletableFuture.completedFuture(json);
            }
//            System.out.println("22222222222");
            Long sourceUid = tradeRequest.getSourceUid();
            Long targetUid = tradeRequest.getTargetUid();
            BigDecimal number = tradeRequest.getAmount();
//            System.out.println("-----------");
            boolean rightnum = isRightNum(number);
            if (!rightnum) {
//                System.out.println("333333333333");
                redisCommands.sadd("tradeRequestId", requestId);
                ApiResponse apiResponse= new ApiResponse("amount is illegal", 201, requestId, null);
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String json = gson.toJson(apiResponse);
                return CompletableFuture.completedFuture(json);
            }
            if (number.signum() == 0) {
                redisCommands.sadd("tradeRequestId", requestId);
                ApiResponse apiResponse=new ApiResponse("ok", 200, requestId, null);
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String json = gson.toJson(apiResponse);
                return CompletableFuture.completedFuture(json);
            }

//            System.out.println("99999999999");
            String hashv=redisCommands.hgetall("user-money").toString();
//            System.out.println(hashv);
//            System.out.println(sourceUid.toString());
            String value = redisCommands.hget("user-money", sourceUid.toString());
//            System.out.println(redisCommands.hget("user-money", sourceUid.toString()));
            BigDecimal sourceValue = new BigDecimal(value);
//            System.out.println("555555555555");
            if (number.compareTo(sourceValue) > 0) {
                redisCommands.sadd("tradeRequestId", requestId);
                ApiResponse apiResponse= new ApiResponse("amount is more than sourceid balance", 205, requestId, null);
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String json = gson.toJson(apiResponse);
                return CompletableFuture.completedFuture(json);
            }
            sourceValue = sourceValue.subtract(number);
            String value2 = redisCommands.hget("user-money", targetUid.toString());
            BigDecimal targetValue = new BigDecimal(value2);
            targetValue = targetValue.add(number);
//            System.out.println("666666666666");
            redisCommands.watch("user-money");
            redisCommands.multi();
            try {
                redisCommands.hset("user-money",sourceUid.toString(),sourceValue.toString());
                redisCommands.hset("user-money",targetUid.toString(),targetValue.toString());
                redisCommands.exec();
                redisCommands.sadd("tradeRequestId", requestId);
                ApiResponse apiResponse= new ApiResponse("ok", 200, requestId, null);
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String json = gson.toJson(apiResponse);
//                System.out.println("777777777777");
                return CompletableFuture.completedFuture(json);
            } catch (Exception e) {
                redisCommands.discard();                          //如果抛出异常则取消执行
                e.printStackTrace();
                throw new RuntimeException(e);
            }finally {
                redisCommands.unwatch();                           //取消监视
                connection.close();
                redisClient.shutdown();                          //关闭连接
            }
        } catch (Exception e) {
            System.err.println("Failed to connect to Redis: " + e.getMessage());
        }
//        System.out.println("88888888888");
        ApiResponse apiResponse= new ApiResponse("Failed to connect to Redis", 301, requestId, null);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(apiResponse);
        return CompletableFuture.completedFuture(json);
    }

    public static boolean isRightNum(BigDecimal number) {
        if (number.signum() == 0) {
            return true; // 如果是零，没有小数
        }
        if (number.scale() > 2)
            return false;
        if (number.compareTo(new BigDecimal("0.01")) < 0 || number.compareTo(new BigDecimal("10000")) > 0)
            return false;
        return true;
    }
}