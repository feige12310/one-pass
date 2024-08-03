package com.ksyun.course;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

import java.io.IOException;

@EnableRedisRepositories
@SpringBootApplication
public class OnePassApplication implements CommandLineRunner {
	@Autowired
	private RedisTest redisTest;

	public static void main(String[] args) {
		SpringApplication.run(OnePassApplication.class, args);
	}
	@Override
	public void run(String... args) throws InterruptedException, IOException {
//		redisTest.sendPostRequest();
//        asyncTest.asynTest();
		redisTest.redisTest();
//        batchPay.batchPay();
	}
}
