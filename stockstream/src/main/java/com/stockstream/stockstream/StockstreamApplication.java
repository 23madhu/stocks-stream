package com.stockstream.stockstream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = "com.stockstream")
@EntityScan(basePackages = "com.stockstream.model")
@EnableJpaRepositories(basePackages = "com.stockstream.repository")
public class StockstreamApplication {

	public static void main(String[] args) {
		SpringApplication.run(StockstreamApplication.class, args);
	}
}