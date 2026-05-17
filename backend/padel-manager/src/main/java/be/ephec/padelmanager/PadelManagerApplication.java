package be.ephec.padelmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PadelManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(PadelManagerApplication.class, args);
	}

}
