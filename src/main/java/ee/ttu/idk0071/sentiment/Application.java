package ee.ttu.idk0071.sentiment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import it.ozimov.springboot.mail.configuration.EnableEmailTools;

@SpringBootApplication
@EnableEmailTools
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
