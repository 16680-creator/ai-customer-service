<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: META-INF/spring/aot.factories -->
<!-- 正文引用：[[源码:aot.factories#L起-L止]] ；代码块头部：(src: aot.factories#L起-L止) -->

# aot.factories

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `META-INF/spring/aot.factories` |
| 行数 | 22 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:33:26.194Z |

```java
    1 | org.springframework.aot.hint.RuntimeHintsRegistrar=\
    2 | org.springframework.boot.ApplicationProperties$ApplicationPropertiesRuntimeHints,\
    3 | org.springframework.boot.SpringApplicationBannerPrinter$SpringApplicationBannerPrinterRuntimeHints,\
    4 | org.springframework.boot.WebApplicationType$WebApplicationTypeRuntimeHints,\
    5 | org.springframework.boot.context.config.ConfigDataLocationRuntimeHints,\
    6 | org.springframework.boot.context.config.ConfigDataPropertiesRuntimeHints,\
    7 | org.springframework.boot.env.PropertySourceRuntimeHints,\
    8 | org.springframework.boot.logging.java.JavaLoggingSystemRuntimeHints,\
    9 | org.springframework.boot.logging.log4j2.Log4J2RuntimeHints,\
   10 | org.springframework.boot.logging.logback.LogbackRuntimeHints,\
   11 | org.springframework.boot.logging.structured.ElasticCommonSchemaProperties$ElasticCommonSchemaPropertiesRuntimeHints,\
   12 | org.springframework.boot.logging.structured.GraylogExtendedLogFormatProperties$GraylogExtendedLogFormatPropertiesRuntimeHints,\
   13 | org.springframework.boot.logging.structured.StructuredLoggingJsonProperties$StructuredLoggingJsonPropertiesRuntimeHints,\
   14 | org.springframework.boot.system.JavaVersion$Hints
   15 | 
   16 | org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor=\
   17 | org.springframework.boot.context.properties.ConfigurationPropertiesBeanFactoryInitializationAotProcessor,\
   18 | org.springframework.boot.logging.structured.StructuredLoggingJsonPropertiesBeanFactoryInitializationAotProcessor,\
   19 | org.springframework.boot.support.EnvironmentPostProcessorApplicationListener$EnvironmentBeanFactoryInitializationAotProcessor
   20 | 
   21 | org.springframework.beans.factory.aot.BeanRegistrationAotProcessor=\
   22 | org.springframework.boot.context.properties.ConfigurationPropertiesBeanRegistrationAotProcessor
```
