package com.ksyun.course;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Service
public class PayOnce {

    @Async("async")
    public CompletableFuture<ClientResponse> sendPostRequestWithRetry(String uuid,WebClient webClient, String url, String requestBody, int retries, Duration timeout) {
        CompletableFuture<ClientResponse> future = new CompletableFuture<>();
//        String uuid=UUID.randomUUID().toString();
        webClient.post()
                .uri(url)
                .header("Content-Type", "application/json")
                .header("X-KSY-KINGSTAR-ID", "40001")
                .header("X-KSY-REQUEST-ID", uuid)
                .bodyValue(requestBody)
                .exchange()
                .doOnError(Exception.class, err -> {  //处理异常
                    System.out.println(LocalDateTime.now() +  "---发生错误：" +err.getMessage() );
                })
                .retry(3)
                .subscribe(response -> future.complete(response));

        return future;
    }
    // 发送 POST 请求
    private static Mono<ClientResponse> sendPostRequest(WebClient webClient, String uri, String requestBody) {
        return webClient.post()
                .uri(uri)
                .body(BodyInserters.fromValue(requestBody))
                .exchange();
    }
}