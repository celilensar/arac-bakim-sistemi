package com.arac_bakim.query_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class QueryApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(QueryApiApplication.class, args);
	}

}
