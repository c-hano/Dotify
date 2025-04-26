package com.example.dotify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class DotifyApplication {

	public static void main(String[] args) {
		SpringApplication.run(DotifyApplication.class, args);
	}

}
