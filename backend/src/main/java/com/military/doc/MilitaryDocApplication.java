package com.military.doc;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan({"com.military.doc.modules.*.mapper", "com.military.doc.ai.mapper"})
public class MilitaryDocApplication {

    public static void main(String[] args) {
        SpringApplication.run(MilitaryDocApplication.class, args);
    }
}