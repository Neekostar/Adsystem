package com.neekostar.adsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@EnableCaching
@EnableRetry
public class AdsystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdsystemApplication.class, args);
    }

}
