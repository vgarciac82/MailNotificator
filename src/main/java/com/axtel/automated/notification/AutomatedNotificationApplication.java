package com.axtel.automated.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.cloud.client.discovery.EnableDiscoveryClient
public class AutomatedNotificationApplication {

	public static void main(String[] args) {
		SpringApplication.run(AutomatedNotificationApplication.class, args);
	}

}
