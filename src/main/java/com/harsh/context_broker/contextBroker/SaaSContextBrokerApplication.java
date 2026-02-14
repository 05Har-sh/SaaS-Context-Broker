package com.harsh.context_broker.contextBroker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SaaSContextBrokerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SaaSContextBrokerApplication.class, args);
	}

}
