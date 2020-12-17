package lifecycle;

import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import util.Log;

/**
 * 　　　　　　　 ┏┓　 ┏┓+ +
 * 　　　　　　　┏┛┻━━━┛┻┓ + +
 * 　　　　　　　┃　　　　　　┃
 * 　　　　　　　┃　　　━　　 ┃ ++ + + +
 * 　　　　　　 ████━████  ┃+
 * 　　　　　　　┃　　　　　　　┃ +
 * 　　　　　　　┃　　　┻　　　┃
 * 　　　　　　　┃　　　　　　┃ + +
 * 　　　　　　　┗━┓　　　┏━┛
 * 　　　　　　　　 ┃　　　┃
 * 　　　　　　　　 ┃　　　┃ + + + +
 * 　　　　　　　　 ┃　　　┃　　　　Code is far away from bug with the animal protecting
 * 　　　　　　　　 ┃　　　┃ + 　　　　神兽保佑,代码无bug
 * 　　　　　　　　 ┃　　　┃
 * 　　　　　　　　 ┃　　　┃　　+
 * 　　　　　　　　 ┃　 　 ┗━━━┓ + +
 * 　　　　　　　　 ┃ 　　　　   ┣┓
 * 　　　　　　　　 ┃ 　　　　　 ┏┛
 * 　　　　　　　　 ┗┓┓┏━┳┓┏┛ + + + +
 * 　　　　　　　　  ┃┫┫ ┃┫┫
 * 　　　　　　　　  ┗┻┛ ┗┻┛+ + + +
 * <p>
 * spring-boot-build
 * 2020-12-13 15:17
 *
 * 需要配置于spring.factories中
 * SpringApplicationRunListener是SpringBoot的生命周期接口
 * 构造器必须要带org.springframework.boot.SpringApplication String[]两个参数
 * @author yaoyy
 */
public class MyImplSpringApplicationRunListener implements SpringApplicationRunListener {
	public MyImplSpringApplicationRunListener(org.springframework.boot.SpringApplication x, String[] xxx) {
		Log.log("hello 王八蛋");
	}

	@Override
	public void starting() {
		Log.log("spring boot自带的RunListener中会执行所有配置在spring.factories的ApplicationListener");
	}

	@Override
	public void environmentPrepared(ConfigurableEnvironment environment) {

	}

	@Override
	public void contextPrepared(ConfigurableApplicationContext context) {

	}

	@Override
	public void contextLoaded(ConfigurableApplicationContext context) {

	}

	@Override
	public void started(ConfigurableApplicationContext context) {

	}

	@Override
	public void running(ConfigurableApplicationContext context) {

	}

	@Override
	public void failed(ConfigurableApplicationContext context, Throwable exception) {

	}
}
