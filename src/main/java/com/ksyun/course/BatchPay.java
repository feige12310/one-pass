package com.ksyun.course;


import com.google.gson.Gson;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RestController
@EnableAsync
@Component
public class BatchPay {

    @Autowired
    private PayOnce payOnce;

//    @Autowired
//    private RedisTemplate<String, Object> redisTemplate;


    @PostMapping("/batchPay")
    public void batchPay(@RequestBody BatchPayRequest request, HttpServletResponse response) throws IOException, InterruptedException, ExecutionException {
        System.out.println("=========here=======");
        String eip = "http://172.16.0.90";
        String url = eip + "/thirdpart/onePass/pay";
        int poolnum = 10;
        BigDecimal money = BigDecimal.valueOf(10000);



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




        String batchpayid = request.getBatchPayId();
        List<Long> uids = request.getUids();

//        boolean idExist = redisTemplate.opsForSet().isMember("batchpayid", batchpayid);
        boolean idExist =redisCommands.sismember("batchpayid", batchpayid);
        if (idExist) {
            System.out.println("==========batchpayid exist=========");
            sendResponse(response);
            //batchPayFinish(eip+"batchPayFinish",batchpayid);
            return;
        }
        System.out.println("========id write to batchpayid======");
        redisCommands.sadd("batchpayid", batchpayid);
//        redisTemplate.opsForSet().add("batchpayid", batchpayid);
        sendResponse(response);
        // 逐个加入Redis列表
        for (Long uid : uids) {
            BigDecimal balance = singlePay(url, uid, poolnum, money);
//            BigDecimal haveMoney= (BigDecimal) redisTemplate.opsForHash().get("user-money", uid.toString());
            String haveMoney=redisCommands.hget("user-money", uid.toString());
            if (haveMoney != null) {
                balance = balance.add(BigDecimal.valueOf(Long.parseLong(haveMoney)));
            }
            System.out.println(balance.toString());
            redisCommands.hset("user-money", uid.toString(), balance.toString());
//            redisTemplate.opsForHash().put("user-money", uid.toString(), balance.toString());
            System.out.println("=============redis in=============");
        }
//        System.out.println(redisTemplate.opsForHash().entries("users"));
        System.out.println("222222222222222");
        batchPayFinish(eip + "/thirdpart/onePass/batchPayFinish", batchpayid);
    }




    public BigDecimal singlePay(String url, Long uid, int poolnum, BigDecimal money) throws InterruptedException, IOException, ExecutionException {
        int maxRetries = 5; // 最大重试次数
        Duration timeout = Duration.ofMillis(100); // 超时时间
        BigDecimal balance;
        List<Integer> wanList = Collections.synchronizedList(new ArrayList<>());
        List<BigDecimal> lessList = Collections.synchronizedList(new ArrayList<>());
//        ExecutorService executor = Executors.newFixedThreadPool(8);
        final boolean[] flag = {true};
        while (flag[0] && money.compareTo(BigDecimal.valueOf(10000)) >= 0) {
            CompletableFuture<ClientResponse>[] futures1 = new CompletableFuture[poolnum];
            int count = poolnum;

            while (count > 0) {
                String uuid = UUID.randomUUID().toString();
                WebClient webClient = WebClient.create();
                String jsonData = "{\"transactionId\": \"" + uuid + "\", \"uid\": " + uid.toString() + ", \"amount\": " + money.toString() + "}";
                String uuid2 = UUID.randomUUID().toString();
                CompletableFuture<ClientResponse> responseMono = payOnce.sendPostRequestWithRetry(uuid2, webClient, url, jsonData, maxRetries, timeout);
                futures1[count - 1] = responseMono;
                count -= 1;
            }
            CompletableFuture.allOf(futures1);
            for (int i = 0; i < futures1.length; i++) {
                Integer statusCode = null;
                try {
                    ClientResponse response = futures1[i].get();
                    String responseInfo = response.bodyToMono(String.class).blockOptional().orElse(null);
                    if (responseInfo != null) {
                        ApiResponse p = new Gson().fromJson(responseInfo, ApiResponse.class);
                        statusCode = p.getCode();
//                        System.out.println("Response status: " + statusCode + " " + responseInfo);
                    } else {
                        System.out.println("Empty or null response body.");
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
                if (statusCode == 200) {
                    wanList.add(1);
                } else if (statusCode == 501) {
                    flag[0] = false;
                } else {
                    System.out.println("Unexpected status code: " + statusCode);
                }
            }
        }
        System.out.println("+++++++++++++" + wanList.size() + " wansize===============");
        while (money.compareTo(BigDecimal.valueOf(0.1)) >= 0) {
            CompletableFuture<ClientResponse>[] futures1 = new CompletableFuture[poolnum];
            int count = poolnum;
            if (flag[0] == false) {
                money = money.divide(BigDecimal.valueOf(10));
                flag[0] = true;
            }
            while (count > 0) {
                String uuid = UUID.randomUUID().toString();
                WebClient webClient = WebClient.create();
                String jsonData = "{\"transactionId\": \"" + uuid + "\", \"uid\": " + uid.toString() + ", \"amount\": " + money.toString() + "}";
                String uuid2 = UUID.randomUUID().toString();
                CompletableFuture<ClientResponse> responseMono = payOnce.sendPostRequestWithRetry(uuid2, webClient, url, jsonData, maxRetries, timeout);
                futures1[count - 1] = responseMono;

                count -= 1;
            }
            for (int i = 0; i < futures1.length; i++) {
                Integer statusCode = null;
                try {
                    ClientResponse response = futures1[i].get();
                    String responseInfo = response.bodyToMono(String.class).blockOptional().orElse(null);
                    if (responseInfo != null) {
                        ApiResponse p = new Gson().fromJson(responseInfo, ApiResponse.class);
                        statusCode = p.getCode();
//                        System.out.println("Response status: " + statusCode + " " + responseInfo);
                    } else {
                        System.out.println("Empty or null response body.");
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
                if (statusCode == 200) {
                    lessList.add(money);
                } else if (statusCode == 501) {
                    flag[0] = false;
                } else {
                    System.out.println("Unexpected status code: " + statusCode);
                }
            }
        }

        BigDecimal sum = lessList.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        balance = BigDecimal.valueOf(wanList.size() * 10000).add(sum);
        System.out.println(uid + " Estimated account balance: " + balance);
        return balance;
    }

    public void sendResponse(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);
        String jsonResponse = "{\"msg\":\"ok\",\"code\":200,\"requestId\":\"" + UUID.randomUUID().toString() + "\",\"data\":null}";

        try {
            response.getWriter().write(jsonResponse);
            response.getWriter().flush();
            response.getWriter().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void batchPayFinish(String url, String batchPayId) throws IOException {
        URL resetUrl = new URL(url);

        HttpURLConnection resetConnection = (HttpURLConnection) resetUrl.openConnection();

        // 设置请求方法为 POST
        resetConnection.setRequestMethod("POST");
        resetConnection.setRequestProperty("Content-Type", "application/json");
        resetConnection.setRequestProperty("X-KSY-KINGSTAR-ID", "40001");

        resetConnection.setRequestProperty("X-KSY-REQUEST-ID", UUID.randomUUID().toString());

        // 构建要传递的 JSON 数据
        String jsonData =  batchPayId;
        //String jsonData = accountInfo;

        // 将数据写入请求体
        resetConnection.setDoOutput(true);
        try (OutputStream os = resetConnection.getOutputStream()) {
            byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        BufferedReader reader2 = new BufferedReader(new InputStreamReader(resetConnection.getInputStream()));
        String line2;
        StringBuffer response2 = new StringBuffer();

        while ((line2 = reader2.readLine()) != null) {
            response2.append(line2);
        }
        System.out.println("batchPayFinish Response: " +response2.toString());
        reader2.close();
        resetConnection.disconnect();
    }
}
