package com.dreamcatcher.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling //  @Scheduled 어노테이션이 동작하게 하려면
public class DreamcatcherApplication {

	public static void main(String[] args) {
		SpringApplication.run(DreamcatcherApplication.class, args);
	}

}
