<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/DispatcherServlet.properties -->
<!-- 正文引用：[[源码:DispatcherServlet.properties#L起-L止]] ；代码块头部：(src: DispatcherServlet.properties#L起-L止) -->

# DispatcherServlet.properties

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/DispatcherServlet.properties` |
| 行数 | 25 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:11:23.618Z |

```java
    1 | # Default implementation classes for DispatcherServlet's strategy interfaces.
    2 | # Used as fallback when no matching beans are found in the DispatcherServlet context.
    3 | # Not meant to be customized by application developers.
    4 | 
    5 | org.springframework.web.servlet.LocaleResolver=org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver
    6 | 
    7 | org.springframework.web.servlet.HandlerMapping=org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping,\
    8 | 	org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping,\
    9 | 	org.springframework.web.servlet.function.support.RouterFunctionMapping
   10 | 
   11 | org.springframework.web.servlet.HandlerAdapter=org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter,\
   12 | 	org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter,\
   13 | 	org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter,\
   14 | 	org.springframework.web.servlet.function.support.HandlerFunctionAdapter
   15 | 
   16 | 
   17 | org.springframework.web.servlet.HandlerExceptionResolver=org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver,\
   18 | 	org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver,\
   19 | 	org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver
   20 | 
   21 | org.springframework.web.servlet.RequestToViewNameTranslator=org.springframework.web.servlet.view.DefaultRequestToViewNameTranslator
   22 | 
   23 | org.springframework.web.servlet.ViewResolver=org.springframework.web.servlet.view.InternalResourceViewResolver
   24 | 
   25 | org.springframework.web.servlet.FlashMapManager=org.springframework.web.servlet.support.SessionFlashMapManager
```
