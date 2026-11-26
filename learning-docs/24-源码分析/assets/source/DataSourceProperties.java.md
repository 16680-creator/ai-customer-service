<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-jdbc:4.2.0 | path: org/springframework/boot/jdbc/autoconfigure/DataSourceProperties.java -->
<!-- 正文引用：[[源码:DataSourceProperties.java#L起-L止]] ；代码块头部：(src: DataSourceProperties.java#L起-L止) -->

# DataSourceProperties.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-jdbc:4.2.0` |
| 路径 | `org/springframework/boot/jdbc/autoconfigure/DataSourceProperties.java` |
| 行数 | 397 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-jdbc/4.2.0/spring-boot-jdbc-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:52.399Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.jdbc.autoconfigure;
    6 | 
    7 | import java.util.LinkedHashMap;
    8 | import java.util.Map;
    9 | import java.util.UUID;
   10 | 
   11 | import javax.sql.DataSource;
   12 | 
   13 | import org.jspecify.annotations.Nullable;
   14 | 
   15 | import org.springframework.beans.factory.BeanClassLoaderAware;
   16 | import org.springframework.beans.factory.BeanCreationException;
   17 | import org.springframework.beans.factory.InitializingBean;
   18 | import org.springframework.boot.context.properties.ConfigurationProperties;
   19 | import org.springframework.boot.jdbc.DataSourceBuilder;
   20 | import org.springframework.boot.jdbc.DatabaseDriver;
   21 | import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
   22 | import org.springframework.util.Assert;
   23 | import org.springframework.util.ClassUtils;
   24 | import org.springframework.util.StringUtils;
   25 | 
   26 | /**
   27 |  * Base class for configuration of a data source.
   28 |  *
   29 |  * @author Dave Syer
   30 |  * @author Maciej Walkowiak
   31 |  * @author Stephane Nicoll
   32 |  * @author Benedikt Ritter
   33 |  * @author Eddú Meléndez
   34 |  * @author Scott Frederick
   35 |  * @since 4.0.0
   36 |  */
   37 | @ConfigurationProperties("spring.datasource")
   38 | public class DataSourceProperties implements BeanClassLoaderAware, InitializingBean {
   39 | 
   40 | 	@SuppressWarnings("NullAway.Init")
   41 | 	private ClassLoader classLoader;
   42 | 
   43 | 	/**
   44 | 	 * Whether to generate a random datasource name.
   45 | 	 */
   46 | 	private boolean generateUniqueName = true;
   47 | 
   48 | 	/**
   49 | 	 * Datasource name to use if "generate-unique-name" is false. Defaults to "testdb"
   50 | 	 * when using an embedded database, otherwise null.
   51 | 	 */
   52 | 	private @Nullable String name;
   53 | 
   54 | 	/**
   55 | 	 * Fully qualified name of the DataSource implementation to use. By default, a
   56 | 	 * connection pool implementation is auto-detected from the classpath.
   57 | 	 */
   58 | 	private @Nullable Class<? extends DataSource> type;
   59 | 
   60 | 	/**
   61 | 	 * Fully qualified name of the JDBC driver. Auto-detected based on the URL by default.
   62 | 	 */
   63 | 	private @Nullable String driverClassName;
   64 | 
   65 | 	/**
   66 | 	 * JDBC URL of the database.
   67 | 	 */
   68 | 	private @Nullable String url;
   69 | 
   70 | 	/**
   71 | 	 * Login username of the database.
   72 | 	 */
   73 | 	private @Nullable String username;
   74 | 
   75 | 	/**
   76 | 	 * Login password of the database.
   77 | 	 */
   78 | 	private @Nullable String password;
   79 | 
   80 | 	/**
   81 | 	 * JNDI location of the datasource. Class, url, username and password are ignored when
   82 | 	 * set.
   83 | 	 */
   84 | 	private @Nullable String jndiName;
   85 | 
   86 | 	/**
   87 | 	 * Connection details for an embedded database. Defaults to the most suitable embedded
   88 | 	 * database that is available on the classpath.
   89 | 	 */
   90 | 	@SuppressWarnings("NullAway.Init")
   91 | 	private EmbeddedDatabaseConnection embeddedDatabaseConnection;
   92 | 
   93 | 	private Xa xa = new Xa();
   94 | 
   95 | 	private @Nullable String uniqueName;
   96 | 
   97 | 	@Override
   98 | 	public void setBeanClassLoader(ClassLoader classLoader) {
   99 | 		this.classLoader = classLoader;
  100 | 	}
  101 | 
  102 | 	@Override
  103 | 	public void afterPropertiesSet() throws Exception {
  104 | 		if (this.embeddedDatabaseConnection == null) {
  105 | 			this.embeddedDatabaseConnection = EmbeddedDatabaseConnection.get(this.classLoader);
  106 | 		}
  107 | 	}
  108 | 
  109 | 	/**
  110 | 	 * Initialize a {@link DataSourceBuilder} with the state of this instance.
  111 | 	 * @return a {@link DataSourceBuilder} initialized with the customizations defined on
  112 | 	 * this instance
  113 | 	 */
  114 | 	public DataSourceBuilder<?> initializeDataSourceBuilder() {
  115 | 		return DataSourceBuilder.create(getClassLoader())
  116 | 			.type(getType())
  117 | 			.driverClassName(determineDriverClassName())
  118 | 			.url(determineUrl())
  119 | 			.username(determineUsername())
  120 | 			.password(determinePassword());
  121 | 	}
  122 | 
  123 | 	public boolean isGenerateUniqueName() {
  124 | 		return this.generateUniqueName;
  125 | 	}
  126 | 
  127 | 	public void setGenerateUniqueName(boolean generateUniqueName) {
  128 | 		this.generateUniqueName = generateUniqueName;
  129 | 	}
  130 | 
  131 | 	public @Nullable String getName() {
  132 | 		return this.name;
  133 | 	}
  134 | 
  135 | 	public void setName(@Nullable String name) {
  136 | 		this.name = name;
  137 | 	}
  138 | 
  139 | 	public @Nullable Class<? extends DataSource> getType() {
  140 | 		return this.type;
  141 | 	}
  142 | 
  143 | 	public void setType(@Nullable Class<? extends DataSource> type) {
  144 | 		this.type = type;
  145 | 	}
  146 | 
  147 | 	/**
  148 | 	 * Return the configured driver or {@code null} if none was configured.
  149 | 	 * @return the configured driver
  150 | 	 * @see #determineDriverClassName()
  151 | 	 */
  152 | 	public @Nullable String getDriverClassName() {
  153 | 		return this.driverClassName;
  154 | 	}
  155 | 
  156 | 	public void setDriverClassName(@Nullable String driverClassName) {
  157 | 		this.driverClassName = driverClassName;
  158 | 	}
  159 | 
  160 | 	/**
  161 | 	 * Determine the driver to use based on this configuration and the environment.
  162 | 	 * @return the driver to use
  163 | 	 */
  164 | 	public String determineDriverClassName() {
  165 | 		String driverClassName = findDriverClassName();
  166 | 		if (!StringUtils.hasText(driverClassName)) {
  167 | 			throw new DataSourceBeanCreationException("Failed to determine a suitable driver class", this,
  168 | 					this.embeddedDatabaseConnection);
  169 | 		}
  170 | 		return driverClassName;
  171 | 	}
  172 | 
  173 | 	@Nullable String findDriverClassName() {
  174 | 		if (StringUtils.hasText(this.driverClassName)) {
  175 | 			Assert.state(driverClassIsLoadable(this.driverClassName),
  176 | 					() -> "Cannot load driver class: " + this.driverClassName);
  177 | 			return this.driverClassName;
  178 | 		}
  179 | 		String driverClassName = null;
  180 | 		if (StringUtils.hasText(this.url)) {
  181 | 			driverClassName = DatabaseDriver.fromJdbcUrl(this.url).getDriverClassName();
  182 | 		}
  183 | 		if (!StringUtils.hasText(driverClassName)) {
  184 | 			driverClassName = this.embeddedDatabaseConnection.getDriverClassName();
  185 | 		}
  186 | 		return driverClassName;
  187 | 	}
  188 | 
  189 | 	private boolean driverClassIsLoadable(String driverClassName) {
  190 | 		try {
  191 | 			ClassUtils.forName(driverClassName, null);
  192 | 			return true;
  193 | 		}
  194 | 		catch (UnsupportedClassVersionError ex) {
  195 | 			// Driver library has been compiled with a later JDK, propagate error
  196 | 			throw ex;
  197 | 		}
  198 | 		catch (Throwable ex) {
  199 | 			return false;
  200 | 		}
  201 | 	}
  202 | 
  203 | 	/**
  204 | 	 * Return the configured url or {@code null} if none was configured.
  205 | 	 * @return the configured url
  206 | 	 * @see #determineUrl()
  207 | 	 */
  208 | 	public @Nullable String getUrl() {
  209 | 		return this.url;
  210 | 	}
  211 | 
  212 | 	public void setUrl(@Nullable String url) {
  213 | 		this.url = url;
  214 | 	}
  215 | 
  216 | 	/**
  217 | 	 * Determine the url to use based on this configuration and the environment.
  218 | 	 * @return the url to use
  219 | 	 */
  220 | 	public String determineUrl() {
  221 | 		if (StringUtils.hasText(this.url)) {
  222 | 			return this.url;
  223 | 		}
  224 | 		String databaseName = determineDatabaseName();
  225 | 		String url = (databaseName != null) ? this.embeddedDatabaseConnection.getUrl(databaseName) : null;
  226 | 		if (!StringUtils.hasText(url)) {
  227 | 			throw new DataSourceBeanCreationException("Failed to determine suitable jdbc url", this,
  228 | 					this.embeddedDatabaseConnection);
  229 | 		}
  230 | 		return url;
  231 | 	}
  232 | 
  233 | 	/**
  234 | 	 * Determine the name to used based on this configuration.
  235 | 	 * @return the database name to use or {@code null}
  236 | 	 */
  237 | 	public @Nullable String determineDatabaseName() {
  238 | 		if (this.generateUniqueName) {
  239 | 			if (this.uniqueName == null) {
  240 | 				this.uniqueName = UUID.randomUUID().toString();
  241 | 			}
  242 | 			return this.uniqueName;
  243 | 		}
  244 | 		if (StringUtils.hasLength(this.name)) {
  245 | 			return this.name;
  246 | 		}
  247 | 		if (this.embeddedDatabaseConnection != EmbeddedDatabaseConnection.NONE) {
  248 | 			return "testdb";
  249 | 		}
  250 | 		return null;
  251 | 	}
  252 | 
  253 | 	/**
  254 | 	 * Return the configured username or {@code null} if none was configured.
  255 | 	 * @return the configured username
  256 | 	 * @see #determineUsername()
  257 | 	 */
  258 | 	public @Nullable String getUsername() {
  259 | 		return this.username;
  260 | 	}
  261 | 
  262 | 	public void setUsername(@Nullable String username) {
  263 | 		this.username = username;
  264 | 	}
  265 | 
  266 | 	/**
  267 | 	 * Determine the username to use based on this configuration and the environment.
  268 | 	 * @return the username to use
  269 | 	 */
  270 | 	public @Nullable String determineUsername() {
  271 | 		if (StringUtils.hasText(this.username)) {
  272 | 			return this.username;
  273 | 		}
  274 | 		if (EmbeddedDatabaseConnection.isEmbedded(findDriverClassName(), determineUrl())) {
  275 | 			return "sa";
  276 | 		}
  277 | 		return null;
  278 | 	}
  279 | 
  280 | 	/**
  281 | 	 * Return the configured password or {@code null} if none was configured.
  282 | 	 * @return the configured password
  283 | 	 * @see #determinePassword()
  284 | 	 */
  285 | 	public @Nullable String getPassword() {
  286 | 		return this.password;
  287 | 	}
  288 | 
  289 | 	public void setPassword(@Nullable String password) {
  290 | 		this.password = password;
  291 | 	}
  292 | 
  293 | 	/**
  294 | 	 * Determine the password to use based on this configuration and the environment.
  295 | 	 * @return the password to use
  296 | 	 */
  297 | 	public @Nullable String determinePassword() {
  298 | 		if (StringUtils.hasText(this.password)) {
  299 | 			return this.password;
  300 | 		}
  301 | 		if (EmbeddedDatabaseConnection.isEmbedded(findDriverClassName(), determineUrl())) {
  302 | 			return "";
  303 | 		}
  304 | 		return null;
  305 | 	}
  306 | 
  307 | 	public @Nullable String getJndiName() {
  308 | 		return this.jndiName;
  309 | 	}
  310 | 
  311 | 	/**
  312 | 	 * Allows the DataSource to be managed by the container and obtained through JNDI. The
  313 | 	 * {@code URL}, {@code driverClassName}, {@code username} and {@code password} fields
  314 | 	 * will be ignored when using JNDI lookups.
  315 | 	 * @param jndiName the JNDI name
  316 | 	 */
  317 | 	public void setJndiName(@Nullable String jndiName) {
  318 | 		this.jndiName = jndiName;
  319 | 	}
  320 | 
  321 | 	public EmbeddedDatabaseConnection getEmbeddedDatabaseConnection() {
  322 | 		return this.embeddedDatabaseConnection;
  323 | 	}
  324 | 
  325 | 	public void setEmbeddedDatabaseConnection(EmbeddedDatabaseConnection embeddedDatabaseConnection) {
  326 | 		this.embeddedDatabaseConnection = embeddedDatabaseConnection;
  327 | 	}
  328 | 
  329 | 	public ClassLoader getClassLoader() {
  330 | 		return this.classLoader;
  331 | 	}
  332 | 
  333 | 	public Xa getXa() {
  334 | 		return this.xa;
  335 | 	}
  336 | 
  337 | 	public void setXa(Xa xa) {
  338 | 		this.xa = xa;
  339 | 	}
  340 | 
  341 | 	/**
  342 | 	 * XA Specific datasource settings.
  343 | 	 */
  344 | 	public static class Xa {
  345 | 
  346 | 		/**
  347 | 		 * XA datasource fully qualified name.
  348 | 		 */
  349 | 		private @Nullable String dataSourceClassName;
  350 | 
  351 | 		/**
  352 | 		 * Properties to pass to the XA data source.
  353 | 		 */
  354 | 		private Map<String, String> properties = new LinkedHashMap<>();
  355 | 
  356 | 		public @Nullable String getDataSourceClassName() {
  357 | 			return this.dataSourceClassName;
  358 | 		}
  359 | 
  360 | 		public void setDataSourceClassName(@Nullable String dataSourceClassName) {
  361 | 			this.dataSourceClassName = dataSourceClassName;
  362 | 		}
  363 | 
  364 | 		public Map<String, String> getProperties() {
  365 | 			return this.properties;
  366 | 		}
  367 | 
  368 | 		public void setProperties(Map<String, String> properties) {
  369 | 			this.properties = properties;
  370 | 		}
  371 | 
  372 | 	}
  373 | 
  374 | 	static class DataSourceBeanCreationException extends BeanCreationException {
  375 | 
  376 | 		private final DataSourceProperties properties;
  377 | 
  378 | 		private final EmbeddedDatabaseConnection connection;
  379 | 
  380 | 		DataSourceBeanCreationException(String message, DataSourceProperties properties,
  381 | 				EmbeddedDatabaseConnection connection) {
  382 | 			super(message);
  383 | 			this.properties = properties;
  384 | 			this.connection = connection;
  385 | 		}
  386 | 
  387 | 		DataSourceProperties getProperties() {
  388 | 			return this.properties;
  389 | 		}
  390 | 
  391 | 		EmbeddedDatabaseConnection getConnection() {
  392 | 			return this.connection;
  393 | 		}
  394 | 
  395 | 	}
  396 | 
  397 | }
```
