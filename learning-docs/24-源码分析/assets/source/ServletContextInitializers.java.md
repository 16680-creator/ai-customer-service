<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-web-server:4.2.0 | path: org/springframework/boot/web/server/servlet/ServletContextInitializers.java -->
<!-- 正文引用：[[源码:ServletContextInitializers.java#L起-L止]] ；代码块头部：(src: ServletContextInitializers.java#L起-L止) -->

# ServletContextInitializers.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-web-server:4.2.0` |
| 路径 | `org/springframework/boot/web/server/servlet/ServletContextInitializers.java` |
| 行数 | 106 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-web-server/4.2.0/spring-boot-web-server-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:54.669Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.web.server.servlet;
    6 | 
    7 | import java.time.Duration;
    8 | import java.util.ArrayList;
    9 | import java.util.Arrays;
   10 | import java.util.Iterator;
   11 | import java.util.LinkedHashSet;
   12 | import java.util.List;
   13 | import java.util.Set;
   14 | 
   15 | import jakarta.servlet.ServletContext;
   16 | import jakarta.servlet.ServletException;
   17 | import jakarta.servlet.SessionCookieConfig;
   18 | import org.jspecify.annotations.Nullable;
   19 | 
   20 | import org.springframework.boot.context.properties.PropertyMapper;
   21 | import org.springframework.boot.web.server.Cookie;
   22 | import org.springframework.boot.web.server.WebServer;
   23 | import org.springframework.boot.web.servlet.ServletContextInitializer;
   24 | import org.springframework.lang.Contract;
   25 | 
   26 | /**
   27 |  * The {@link ServletContextInitializer ServletContextInitializers} to apply to a servlet
   28 |  * {@link WebServer}.
   29 |  *
   30 |  * @author Andy Wilkinson
   31 |  * @since 4.0.0
   32 |  */
   33 | public final class ServletContextInitializers implements Iterable<ServletContextInitializer> {
   34 | 
   35 | 	private final List<ServletContextInitializer> initializers;
   36 | 
   37 | 	private ServletContextInitializers(List<ServletContextInitializer> initializers) {
   38 | 		this.initializers = initializers;
   39 | 	}
   40 | 
   41 | 	@Override
   42 | 	public Iterator<ServletContextInitializer> iterator() {
   43 | 		return this.initializers.iterator();
   44 | 	}
   45 | 
   46 | 	/**
   47 | 	 * Creates a new instance from the given {@code settings} and {@code initializers}.
   48 | 	 * @param settings the settings
   49 | 	 * @param initializers the initializers
   50 | 	 * @return the new instance
   51 | 	 */
   52 | 	public static ServletContextInitializers from(ServletWebServerSettings settings,
   53 | 			ServletContextInitializer... initializers) {
   54 | 		List<ServletContextInitializer> mergedInitializers = new ArrayList<>();
   55 | 		mergedInitializers
   56 | 			.add((servletContext) -> settings.getInitParameters().forEach(servletContext::setInitParameter));
   57 | 		mergedInitializers.add(new SessionConfiguringInitializer(settings.getSession()));
   58 | 		mergedInitializers.addAll(Arrays.asList(initializers));
   59 | 		mergedInitializers.addAll(settings.getInitializers());
   60 | 		return new ServletContextInitializers(mergedInitializers);
   61 | 	}
   62 | 
   63 | 	private static final class SessionConfiguringInitializer implements ServletContextInitializer {
   64 | 
   65 | 		private final Session session;
   66 | 
   67 | 		private SessionConfiguringInitializer(Session session) {
   68 | 			this.session = session;
   69 | 		}
   70 | 
   71 | 		@Override
   72 | 		public void onStartup(ServletContext servletContext) throws ServletException {
   73 | 			if (this.session.getTrackingModes() != null) {
   74 | 				servletContext.setSessionTrackingModes(unwrap(this.session.getTrackingModes()));
   75 | 			}
   76 | 			configureSessionCookie(servletContext.getSessionCookieConfig());
   77 | 		}
   78 | 
   79 | 		private void configureSessionCookie(SessionCookieConfig config) {
   80 | 			Cookie cookie = this.session.getCookie();
   81 | 			PropertyMapper map = PropertyMapper.get();
   82 | 			map.from(cookie::getName).to(config::setName);
   83 | 			map.from(cookie::getDomain).to(config::setDomain);
   84 | 			map.from(cookie::getPath).to(config::setPath);
   85 | 			map.from(cookie::getHttpOnly).to(config::setHttpOnly);
   86 | 			map.from(cookie::getSecure).to(config::setSecure);
   87 | 			map.from(cookie::getMaxAge).asInt(Duration::getSeconds).to(config::setMaxAge);
   88 | 			map.from(cookie::getPartitioned).whenTrue().to((partitioned) -> config.setAttribute("Partitioned", ""));
   89 | 		}
   90 | 
   91 | 		@Contract("!null -> !null")
   92 | 		private @Nullable Set<jakarta.servlet.SessionTrackingMode> unwrap(
   93 | 				@Nullable Set<Session.SessionTrackingMode> modes) {
   94 | 			if (modes == null) {
   95 | 				return null;
   96 | 			}
   97 | 			Set<jakarta.servlet.SessionTrackingMode> result = new LinkedHashSet<>();
   98 | 			for (Session.SessionTrackingMode mode : modes) {
   99 | 				result.add(jakarta.servlet.SessionTrackingMode.valueOf(mode.name()));
  100 | 			}
  101 | 			return result;
  102 | 		}
  103 | 
  104 | 	}
  105 | 
  106 | }
```
