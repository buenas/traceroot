package com.traceroot.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
@ConfigurationPropertiesScan
public class TraceRootApplication {

	public static void main(String[] args) {
		SpringApplication.run(TraceRootApplication.class, args);
	}

}
