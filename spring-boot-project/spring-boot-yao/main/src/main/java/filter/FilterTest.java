package filter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.filter.GenericFilterBean;
import util.Log;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * 2020-12-29 12:31
 *
 * @author yaoyy
 */
@Controller
@SpringBootApplication
public class FilterTest {

	@GetMapping("/hello")
	public String hello() {
		return "hello";
	}



	public static void main(String[] args) {
		SpringApplication.run(FilterTest.class);
	}
	@Component
	public static class filter extends GenericFilterBean{

		public filter(){
			Log.log("过滤器的构造器");
		}

		@Override
		public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
			Log.log("过滤器");
		}
	}
}
