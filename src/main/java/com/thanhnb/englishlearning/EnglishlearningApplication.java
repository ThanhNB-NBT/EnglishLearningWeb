package com.thanhnb.englishlearning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

import com.thanhnb.englishlearning.config.JwtProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
@EnableAsync
public class EnglishlearningApplication {

	public static void main(String[] args) {
		SpringApplication.run(EnglishlearningApplication.class, args);
	}

}