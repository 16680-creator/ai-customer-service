<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-security:4.2.0 | path: org/springframework/boot/security/autoconfigure/SecurityProperties.java -->
<!-- 正文引用：[[源码:SecurityProperties.java#L起-L止]] ；代码块头部：(src: SecurityProperties.java#L起-L止) -->

# SecurityProperties.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-security:4.2.0` |
| 路径 | `org/springframework/boot/security/autoconfigure/SecurityProperties.java` |
| 行数 | 84 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-security/4.2.0/spring-boot-security-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:01.057Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.security.autoconfigure;
    6 | 
    7 | import java.util.ArrayList;
    8 | import java.util.List;
    9 | import java.util.UUID;
   10 | 
   11 | import org.springframework.boot.context.properties.ConfigurationProperties;
   12 | import org.springframework.util.StringUtils;
   13 | 
   14 | /**
   15 |  * Configuration properties for Spring Security.
   16 |  *
   17 |  * @author Dave Syer
   18 |  * @author Andy Wilkinson
   19 |  * @author Madhura Bhave
   20 |  * @since 4.0.0
   21 |  */
   22 | @ConfigurationProperties("spring.security")
   23 | public class SecurityProperties {
   24 | 
   25 | 	private final User user = new User();
   26 | 
   27 | 	public User getUser() {
   28 | 		return this.user;
   29 | 	}
   30 | 
   31 | 	public static class User {
   32 | 
   33 | 		/**
   34 | 		 * Default user name.
   35 | 		 */
   36 | 		private String name = "user";
   37 | 
   38 | 		/**
   39 | 		 * Password for the default user name.
   40 | 		 */
   41 | 		private String password = UUID.randomUUID().toString();
   42 | 
   43 | 		/**
   44 | 		 * Granted roles for the default user name.
   45 | 		 */
   46 | 		private List<String> roles = new ArrayList<>();
   47 | 
   48 | 		private boolean passwordGenerated = true;
   49 | 
   50 | 		public String getName() {
   51 | 			return this.name;
   52 | 		}
   53 | 
   54 | 		public void setName(String name) {
   55 | 			this.name = name;
   56 | 		}
   57 | 
   58 | 		public String getPassword() {
   59 | 			return this.password;
   60 | 		}
   61 | 
   62 | 		public void setPassword(String password) {
   63 | 			if (!StringUtils.hasLength(password)) {
   64 | 				return;
   65 | 			}
   66 | 			this.passwordGenerated = false;
   67 | 			this.password = password;
   68 | 		}
   69 | 
   70 | 		public List<String> getRoles() {
   71 | 			return this.roles;
   72 | 		}
   73 | 
   74 | 		public void setRoles(List<String> roles) {
   75 | 			this.roles = new ArrayList<>(roles);
   76 | 		}
   77 | 
   78 | 		public boolean isPasswordGenerated() {
   79 | 			return this.passwordGenerated;
   80 | 		}
   81 | 
   82 | 	}
   83 | 
   84 | }
```
