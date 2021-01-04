package undertow;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.ProxyHandler;
import io.undertow.servlet.api.DeploymentManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.web.embedded.undertow.UndertowBuilderCustomizer;
import org.springframework.boot.web.embedded.undertow.UndertowDeploymentInfoCustomizer;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServer;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.Compression;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.xnio.Options;
import org.xnio.SslClientAuthMode;

import javax.servlet.Servlet;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.stream.Collectors;

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
 * 2020-12-17 10:29
 *
 * 替换spring boot自带的UndertowServletWebServerFactory，因为自带工厂配置没有设置优先级所以将我们自定义的优先级设置比默认高即可，
 * 加载到自带的工厂，发现容器中已经又ServletWebServerFactory了，所以会忽略自带的工厂。
 * 我们自定义的工厂直接继承自带的就好，只重写getUndertowWebServer方法，修改最终提供的UndertowServletWebServer，所以我们还需要自定义
 * 一个UndertowServletWebServer，也直接继承默认的，重写start方法。start是最后服务器启动的方法，里面生成了servlet的handler，然而该类中大部分
 * 方法都是私有的，所以在我们自定义的start方法中只能以hack的形式拿到我们需要修改的值或需要调用的方法（当然，另外一种解决方案则是自己修改undertow
 * spring starter的源码，可是我们只需要定制化简单的一个功能，所以不必大动干戈）。通过反射调用父类中的私有方法createUndertowServer，该方法里
 * 生成了servlet的handler，并做了两层包裹，赋给了undertowBuilder，然后直接build出undertow实例，好在build之前会将undertowBuilder保存在
 * 父类的私有成员中，这给我们hack提供了很好的机会。调用完该方法后再通过反射拿到父类中的builder，再反射拿到builder中的handler，这个handler就是
 * 我们最终需要个性化定制的对象。将该handler使用ProxyHandler包裹一层（此处不懂可以看undertow的handler用法），再set进我们刚刚拿到的
 * undertowBuilder对象，然后build出我们自己的undertow实例，最后按照父类的start方法中的代码启动undertow，至此我们就将自己的handler加入到
 * 了spring boot嵌入式的undertow中，最后别忘了将我们自己生成的undertow hack set到父类中。
 *
 * @author yaoyy
 */
@SpringBootApplication
public class Runner {

	public static void main(String[] args) {
		SpringApplication.run(Runner.class);
	}

	/**
	 * 以下是copy自org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryConfiguration
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({Servlet.class, Undertow.class, SslClientAuthMode.class})
	@ConditionalOnMissingBean(value = ServletWebServerFactory.class, search = SearchStrategy.CURRENT)
	@Order(Ordered.LOWEST_PRECEDENCE - 1)
	static class EmbeddedUndertow {

		@Bean
		UndertowServletWebServerFactory undertowServletWebServerFactory(
				ObjectProvider<UndertowDeploymentInfoCustomizer> deploymentInfoCustomizers,
				ObjectProvider<UndertowBuilderCustomizer> builderCustomizers) {

			// 自定义WebServerFactory
			UndertowServletWebServerFactory factory = new UndertowServletWebServerFactory() {
				/**
				 * 重写getUndertowWebServer，改变最后创建的WebServer对象
				 */
				@Override
				protected UndertowServletWebServer getUndertowWebServer(Undertow.Builder builder, DeploymentManager manager, int port) {
					return new CustomizerUndertowServletWebServer(builder, manager, getContextPath(), isUseForwardHeaders(), port >= 0,
							getCompression(), getServerHeader());
				}
			};

			factory.getDeploymentInfoCustomizers()
					.addAll(deploymentInfoCustomizers.orderedStream().collect(Collectors.toList()));
			factory.getBuilderCustomizers().addAll(builderCustomizers.orderedStream().collect(Collectors.toList()));
			return factory;
		}

	}

	static class CustomizerUndertowServletWebServer extends UndertowServletWebServer {

		public CustomizerUndertowServletWebServer(Undertow.Builder builder, DeploymentManager manager, String contextPath, boolean useForwardHeaders, boolean autoStart, Compression compression, String serverHeader) {
			super(builder, manager, contextPath, useForwardHeaders, autoStart, compression, serverHeader);
		}

		@Override
		public void start() throws WebServerException {
			hackCreateUndertowServer();
			Undertow.Builder builder = hackGetBuilder();
			HttpHandler httpHandler = hackBuilderExtractHandler(builder);

			MyProxyClient myProxyClient = new MyProxyClient();
			myProxyClient.addHost(URI.create("http://192.168.109.132:90"));

			ProxyHandler proxyHandler = Handlers.proxyHandler(myProxyClient, httpHandler);
			builder.setHandler(proxyHandler);

			Undertow undertow = builder.build();
			hackSetInstance(undertow);
			synchronized (hackGetMonitor()) {
				undertow.start();
			}
		}

		private Undertow.Builder hackGetBuilder() {
			Field hackBuilder = ReflectionUtils.findField(super.getClass(), "builder");
			Assert.notNull(hackBuilder, "找到不到builder");
			ReflectionUtils.makeAccessible(hackBuilder);
			return (Undertow.Builder) ReflectionUtils.getField(hackBuilder, this);
		}

		private void hackCreateUndertowServer() {
			Method hackCreateUndertowServer = ReflectionUtils.findMethod(super.getClass(), "createUndertowServer");
			Assert.notNull(hackCreateUndertowServer, "找到不到createUndertowServer方法");
			ReflectionUtils.makeAccessible(hackCreateUndertowServer);
			ReflectionUtils.invokeMethod(hackCreateUndertowServer, this);
		}

		private HttpHandler hackBuilderExtractHandler(Undertow.Builder builder) {
			Field hackHandler = ReflectionUtils.findField(Undertow.Builder.class, "handler");
			Assert.notNull(hackHandler, "找到不到handler");
			ReflectionUtils.makeAccessible(hackHandler);
			return (HttpHandler) ReflectionUtils.getField(hackHandler, builder);
		}

		private void hackSetInstance(Undertow undertow) {
			Field hackUndertow = ReflectionUtils.findField(super.getClass(), "undertow");
			Assert.notNull(hackUndertow, "无法设置undertow");
			ReflectionUtils.makeAccessible(hackUndertow);
			ReflectionUtils.setField(hackUndertow, this, undertow);
		}

		private Object hackGetMonitor() {
			Field hackMonitor = ReflectionUtils.findField(super.getClass(), "monitor");
			Assert.notNull(hackMonitor, "无法获取monitor");
			ReflectionUtils.makeAccessible(hackMonitor);
			return ReflectionUtils.getField(hackMonitor, this);
		}

		static class MyProxyClient extends LoadBalancingProxyClient {
			@Override
			public ProxyTarget findTarget(HttpServerExchange exchange) {
				if ("/xxx".equals(exchange.getRequestPath())) {
					exchange.setRelativePath("/");
					return super.findTarget(exchange);
				}
				return null;
			}
		}
	}
}
