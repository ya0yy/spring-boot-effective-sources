package env;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

/**
 * 2021-01-13 10:25
 *
 * @author yaoyy
 */
@SpringBootApplication
public class EnvironmentTest {

	public static void main(String[] args) {
		ConfigurableApplicationContext run = SpringApplication.run(EnvironmentTest.class);
		Environment bean = run.getBean(Environment.class);
		System.out.println(bean.getClass());
		System.out.println(bean.getProperty("random.server"));
	}
}
