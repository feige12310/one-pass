package com.ksyun.course;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
public class UserAmountController {

    @PostMapping("/onePass/queryUserAmount")
    public String queryUserAmount(@RequestBody List<Long> uids, @RequestHeader("X-KSY-REQUEST-ID") String requestId) {
        // 模拟查询用户账户金额
        if(!(uids != null && uids.size() != 0))
        {
            String uuid = UUID.randomUUID().toString();
            AmountApiResponse response = new AmountApiResponse(510, "user list is illegal", uuid, null);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(response);
            return json;
        }
        List<UserAmount> userAmountList = new ArrayList<>();
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
//        boolean exists = redisCommands.sismember("queryId",requestId);




        for (Long uid : uids) {

            // 这里可以根据 uid 查询用户账户金额，这里仅做演示，直接返回固定的金额

            BigDecimal amount=new BigDecimal(redisCommands.hget("user-money",uid.toString()));
            userAmountList.add(new UserAmount(uid, amount));
        }

        // 构建响应数据
        String uuid = UUID.randomUUID().toString();
        AmountApiResponse response = new AmountApiResponse(200, "ok", uuid, userAmountList);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(response);
        return json;
    }



    // 内部类，用于存储用户账户金额信息
    static class UserAmount {
        private Long uid;
        private BigDecimal amount;

        public UserAmount(Long uid, BigDecimal amount) {
            this.uid = uid;
            this.amount = amount;
        }

        // 省略 getter 和 setter 方法
    }

    // 内部类，用于构建响应数据
    static class AmountApiResponse {
        private int code;
        private String msg;
        private String requestId;
        private List<UserAmount> data;

        public AmountApiResponse(int code, String msg, String requestId, List<UserAmount> data) {
            this.code = code;
            this.msg = msg;
            this.requestId = requestId;
            this.data = data;
        }

        // 省略 getter 和 setter 方法
    }
}