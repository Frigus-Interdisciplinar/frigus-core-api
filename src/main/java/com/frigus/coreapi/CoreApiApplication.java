package com.frigus.coreapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// google cloud e redis desativados temporiaramente para o projeto rodar, depois adicionaremos
@SpringBootApplication(excludeName = {
        "com.google.cloud.spring.autoconfigure.core.GcpContextAutoConfiguration",
        "com.google.cloud.spring.autoconfigure.storage.GcpStorageAutoConfiguration",

        "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
        "org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration",
        "org.springframework.boot.autoconfigure.session.SessionAutoConfiguration"
})
public class CoreApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoreApiApplication.class, args);
    }

}
