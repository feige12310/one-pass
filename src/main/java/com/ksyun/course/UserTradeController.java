package com.ksyun.course;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
public class UserTradeController {

    @Autowired
    private UserTradeService userTradeService;

    @PostMapping("/onePass/userTrade")
    public String userTrade(@RequestHeader("X-KSY-REQUEST-ID") String requestId, @RequestBody TradeRequest tradeRequest) {
        return tradeTask(requestId, tradeRequest);
    }
    public String tradeTask(String requestId,TradeRequest tradeRequest) {
        CompletableFuture<String> responseMono = userTradeService.transfer(requestId, tradeRequest);

        return responseMono.join();
    }
}