package com.traceroot.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class TraceRootApplication {

	public static void main(String[] args) {
		SpringApplication.run(TraceRootApplication.class, args);
	}

}
