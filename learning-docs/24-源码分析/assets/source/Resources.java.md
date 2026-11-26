<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/io/Resources.java -->
<!-- 正文引用：[[源码:Resources.java#L起-L止]] ；代码块头部：(src: Resources.java#L起-L止) -->

# Resources.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/io/Resources.java` |
| 行数 | 340 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:24.032Z |

```java
    1 | /*
    2 |  *    Copyright 2009-2026 the original author or authors.
    3 |  *
    4 |  *    Licensed under the Apache License, Version 2.0 (the "License");
    5 |  *    you may not use this file except in compliance with the License.
    6 |  *    You may obtain a copy of the License at
    7 |  *
    8 |  *       https://www.apache.org/licenses/LICENSE-2.0
    9 |  *
   10 |  *    Unless required by applicable law or agreed to in writing, software
   11 |  *    distributed under the License is distributed on an "AS IS" BASIS,
   12 |  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   13 |  *    See the License for the specific language governing permissions and
   14 |  *    limitations under the License.
   15 |  */
   16 | package org.apache.ibatis.io;
   17 | 
   18 | import java.io.File;
   19 | import java.io.IOException;
   20 | import java.io.InputStream;
   21 | import java.io.InputStreamReader;
   22 | import java.io.Reader;
   23 | import java.net.URI;
   24 | import java.net.URISyntaxException;
   25 | import java.net.URL;
   26 | import java.net.URLConnection;
   27 | import java.nio.charset.Charset;
   28 | import java.nio.file.Path;
   29 | import java.util.Properties;
   30 | 
   31 | /**
   32 |  * A class to simplify access to resources through the classloader.
   33 |  *
   34 |  * @author Clinton Begin
   35 |  */
   36 | public class Resources {
   37 | 
   38 |   private static final ClassLoaderWrapper classLoaderWrapper = new ClassLoaderWrapper();
   39 | 
   40 |   /**
   41 |    * Charset to use when calling getResourceAsReader. null means use the system default.
   42 |    */
   43 |   private static Charset charset;
   44 | 
   45 |   private Resources() {
   46 |   }
   47 | 
   48 |   /**
   49 |    * Returns the default classloader (may be null).
   50 |    *
   51 |    * @return The default classloader
   52 |    */
   53 |   public static ClassLoader getDefaultClassLoader() {
   54 |     return classLoaderWrapper.defaultClassLoader;
   55 |   }
   56 | 
   57 |   /**
   58 |    * Sets the default classloader
   59 |    *
   60 |    * @param defaultClassLoader
   61 |    *          - the new default ClassLoader
   62 |    */
   63 |   public static void setDefaultClassLoader(ClassLoader defaultClassLoader) {
   64 |     classLoaderWrapper.defaultClassLoader = defaultClassLoader;
   65 |   }
   66 | 
   67 |   /**
   68 |    * Returns the URL of the resource on the classpath
   69 |    *
   70 |    * @param resource
   71 |    *          The resource to find
   72 |    *
   73 |    * @return The resource
   74 |    *
   75 |    * @throws java.io.IOException
   76 |    *           If the resource cannot be found or read
   77 |    */
   78 |   public static URL getResourceURL(String resource) throws IOException {
   79 |     // issue #625
   80 |     return getResourceURL(null, resource);
   81 |   }
   82 | 
   83 |   /**
   84 |    * Returns the URL of the resource on the classpath
   85 |    *
   86 |    * @param loader
   87 |    *          The classloader used to fetch the resource
   88 |    * @param resource
   89 |    *          The resource to find
   90 |    *
   91 |    * @return The resource
   92 |    *
   93 |    * @throws java.io.IOException
   94 |    *           If the resource cannot be found or read
   95 |    */
   96 |   public static URL getResourceURL(ClassLoader loader, String resource) throws IOException {
   97 |     URL url = classLoaderWrapper.getResourceAsURL(resource, loader);
   98 |     if (url == null) {
   99 |       throw new IOException("Could not find resource " + resource);
  100 |     }
  101 |     return url;
  102 |   }
  103 | 
  104 |   /**
  105 |    * Returns a resource on the classpath as a Stream object
  106 |    *
  107 |    * @param resource
  108 |    *          The resource to find
  109 |    *
  110 |    * @return The resource
  111 |    *
  112 |    * @throws java.io.IOException
  113 |    *           If the resource cannot be found or read
  114 |    */
  115 |   public static InputStream getResourceAsStream(String resource) throws IOException {
  116 |     return getResourceAsStream(null, resource);
  117 |   }
  118 | 
  119 |   /**
  120 |    * Returns a resource on the classpath as a Stream object
  121 |    *
  122 |    * @param loader
  123 |    *          The classloader used to fetch the resource
  124 |    * @param resource
  125 |    *          The resource to find
  126 |    *
  127 |    * @return The resource
  128 |    *
  129 |    * @throws java.io.IOException
  130 |    *           If the resource cannot be found or read
  131 |    */
  132 |   public static InputStream getResourceAsStream(ClassLoader loader, String resource) throws IOException {
  133 |     InputStream in = classLoaderWrapper.getResourceAsStream(resource, loader);
  134 |     if (in == null) {
  135 |       throw new IOException("Could not find resource " + resource);
  136 |     }
  137 |     return in;
  138 |   }
  139 | 
  140 |   /**
  141 |    * Returns a resource on the classpath as a Properties object
  142 |    *
  143 |    * @param resource
  144 |    *          The resource to find
  145 |    *
  146 |    * @return The resource
  147 |    *
  148 |    * @throws java.io.IOException
  149 |    *           If the resource cannot be found or read
  150 |    */
  151 |   public static Properties getResourceAsProperties(String resource) throws IOException {
  152 |     Properties props = new Properties();
  153 |     try (InputStream in = getResourceAsStream(resource)) {
  154 |       props.load(in);
  155 |     }
  156 |     return props;
  157 |   }
  158 | 
  159 |   /**
  160 |    * Returns a resource on the classpath as a Properties object
  161 |    *
  162 |    * @param loader
  163 |    *          The classloader used to fetch the resource
  164 |    * @param resource
  165 |    *          The resource to find
  166 |    *
  167 |    * @return The resource
  168 |    *
  169 |    * @throws java.io.IOException
  170 |    *           If the resource cannot be found or read
  171 |    */
  172 |   public static Properties getResourceAsProperties(ClassLoader loader, String resource) throws IOException {
  173 |     Properties props = new Properties();
  174 |     try (InputStream in = getResourceAsStream(loader, resource)) {
  175 |       props.load(in);
  176 |     }
  177 |     return props;
  178 |   }
  179 | 
  180 |   /**
  181 |    * Returns a resource on the classpath as a Reader object
  182 |    *
  183 |    * @param resource
  184 |    *          The resource to find
  185 |    *
  186 |    * @return The resource
  187 |    *
  188 |    * @throws java.io.IOException
  189 |    *           If the resource cannot be found or read
  190 |    */
  191 |   public static Reader getResourceAsReader(String resource) throws IOException {
  192 |     Reader reader;
  193 |     if (charset == null) {
  194 |       reader = new InputStreamReader(getResourceAsStream(resource));
  195 |     } else {
  196 |       reader = new InputStreamReader(getResourceAsStream(resource), charset);
  197 |     }
  198 |     return reader;
  199 |   }
  200 | 
  201 |   /**
  202 |    * Returns a resource on the classpath as a Reader object
  203 |    *
  204 |    * @param loader
  205 |    *          The classloader used to fetch the resource
  206 |    * @param resource
  207 |    *          The resource to find
  208 |    *
  209 |    * @return The resource
  210 |    *
  211 |    * @throws java.io.IOException
  212 |    *           If the resource cannot be found or read
  213 |    */
  214 |   public static Reader getResourceAsReader(ClassLoader loader, String resource) throws IOException {
  215 |     Reader reader;
  216 |     if (charset == null) {
  217 |       reader = new InputStreamReader(getResourceAsStream(loader, resource));
  218 |     } else {
  219 |       reader = new InputStreamReader(getResourceAsStream(loader, resource), charset);
  220 |     }
  221 |     return reader;
  222 |   }
  223 | 
  224 |   /**
  225 |    * Returns a resource on the classpath as a File object.
  226 |    *
  227 |    * @param resource
  228 |    *          The resource to find
  229 |    *
  230 |    * @return The resource
  231 |    *
  232 |    * @throws IOException
  233 |    *           Signals that an I/O exception has occurred.
  234 |    * @throws URISyntaxException
  235 |    *           the URI syntax exception
  236 |    */
  237 |   public static File getResourceAsFile(String resource) throws IOException, URISyntaxException {
  238 |     return Path.of(getResourceURL(resource).toURI()).toFile();
  239 |   }
  240 | 
  241 |   /**
  242 |    * Returns a resource on the classpath as a File object.
  243 |    *
  244 |    * @param loader
  245 |    *          - the classloader used to fetch the resource
  246 |    * @param resource
  247 |    *          - the resource to find
  248 |    *
  249 |    * @return The resource
  250 |    *
  251 |    * @throws IOException
  252 |    *           Signals that an I/O exception has occurred.
  253 |    * @throws URISyntaxException
  254 |    *           the URI syntax exception
  255 |    */
  256 |   public static File getResourceAsFile(ClassLoader loader, String resource) throws IOException, URISyntaxException {
  257 |     return Path.of(getResourceURL(loader, resource).toURI()).toFile();
  258 |   }
  259 | 
  260 |   /**
  261 |    * Gets a URL as an input stream
  262 |    *
  263 |    * @param urlString
  264 |    *          - the URL to get
  265 |    *
  266 |    * @return An input stream with the data from the URL
  267 |    *
  268 |    * @throws java.io.IOException
  269 |    *           If the resource cannot be found or read
  270 |    */
  271 |   public static InputStream getUrlAsStream(String urlString) throws IOException {
  272 |     URL url = URI.create(urlString).toURL();
  273 |     URLConnection conn = url.openConnection();
  274 |     return conn.getInputStream();
  275 |   }
  276 | 
  277 |   /**
  278 |    * Gets a URL as a Reader
  279 |    *
  280 |    * @param urlString
  281 |    *          - the URL to get
  282 |    *
  283 |    * @return A Reader with the data from the URL
  284 |    *
  285 |    * @throws java.io.IOException
  286 |    *           If the resource cannot be found or read
  287 |    */
  288 |   public static Reader getUrlAsReader(String urlString) throws IOException {
  289 |     Reader reader;
  290 |     if (charset == null) {
  291 |       reader = new InputStreamReader(getUrlAsStream(urlString));
  292 |     } else {
  293 |       reader = new InputStreamReader(getUrlAsStream(urlString), charset);
  294 |     }
  295 |     return reader;
  296 |   }
  297 | 
  298 |   /**
  299 |    * Gets a URL as a Properties object
  300 |    *
  301 |    * @param urlString
  302 |    *          - the URL to get
  303 |    *
  304 |    * @return A Properties object with the data from the URL
  305 |    *
  306 |    * @throws java.io.IOException
  307 |    *           If the resource cannot be found or read
  308 |    */
  309 |   public static Properties getUrlAsProperties(String urlString) throws IOException {
  310 |     Properties props = new Properties();
  311 |     try (InputStream in = getUrlAsStream(urlString)) {
  312 |       props.load(in);
  313 |     }
  314 |     return props;
  315 |   }
  316 | 
  317 |   /**
  318 |    * Loads a class
  319 |    *
  320 |    * @param className
  321 |    *          - the class to fetch
  322 |    *
  323 |    * @return The loaded class
  324 |    *
  325 |    * @throws ClassNotFoundException
  326 |    *           If the class cannot be found (duh!)
  327 |    */
  328 |   public static Class<?> classForName(String className) throws ClassNotFoundException {
  329 |     return classLoaderWrapper.classForName(className);
  330 |   }
  331 | 
  332 |   public static Charset getCharset() {
  333 |     return charset;
  334 |   }
  335 | 
  336 |   public static void setCharset(Charset charset) {
  337 |     Resources.charset = charset;
  338 |   }
  339 | 
  340 | }
```
