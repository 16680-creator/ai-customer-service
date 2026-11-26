<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-dependencies:2024.0.0 | path: spring-cloud-dependencies-2024.0.0.pom -->
<!-- 正文引用：[[源码:spring-cloud-dependencies-2024.0.0.pom#L起-L止]] ；代码块头部：(src: spring-cloud-dependencies-2024.0.0.pom#L起-L止) -->

# spring-cloud-dependencies-2024.0.0.pom

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-dependencies:2024.0.0` |
| 路径 | `spring-cloud-dependencies-2024.0.0.pom` |
| 行数 | 264 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-dependencies/2024.0.0/spring-cloud-dependencies-2024.0.0.pom |
| 抓取时间 | 2026-11-26T00:12:30.000Z |

```java
    1 | <?xml version="1.0" encoding="UTF-8"?>
    2 | <project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
    3 |     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    4 |   <modelVersion>4.0.0</modelVersion>
    5 |   <parent>
    6 |     <groupId>org.springframework.cloud</groupId>
    7 |     <artifactId>spring-cloud-dependencies-parent</artifactId>
    8 |     <version>4.2.0</version>
    9 |     <relativePath></relativePath>
   10 |   </parent>
   11 |   <groupId>org.springframework.cloud</groupId>
   12 |   <artifactId>spring-cloud-dependencies</artifactId>
   13 |   <version>2024.0.0</version>
   14 |   <packaging>pom</packaging>
   15 |   <name>spring-cloud-dependencies</name>
   16 |   <description>Spring Cloud Dependencies</description>
   17 |   <url>https://spring.io/projects/spring-cloud</url>
   18 |   <organization>
   19 |     <name>Pivotal Software, Inc.</name>
   20 |     <url>https://www.spring.io</url>
   21 |   </organization>
   22 |   <licenses>
   23 |     <license>
   24 |       <name>Apache License, Version 2.0</name>
   25 |       <url>https://www.apache.org/licenses/LICENSE-2.0</url>
   26 |       <comments>Copyright 2014-2021 the original author or authors.
   27 | 
   28 | 				Licensed under the Apache License, Version 2.0 (the "License");
   29 | 				you may not use this file except in compliance with the License.
   30 | 				You may obtain a copy of the License at
   31 | 
   32 | 				https://www.apache.org/licenses/LICENSE-2.0
   33 | 
   34 | 				Unless required by applicable law or agreed to in writing, software
   35 | 				distributed under the License is distributed on an "AS IS" BASIS,
   36 | 				WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
   37 | 				implied.
   38 | 
   39 | 				See the License for the specific language governing permissions and
   40 | 				limitations under the License.</comments>
   41 |     </license>
   42 |   </licenses>
   43 |   <developers>
   44 |     <developer>
   45 |       <id>dsyer</id>
   46 |       <name>Dave Syer</name>
   47 |       <email>dsyer at pivotal.io</email>
   48 |       <organization>Pivotal Software, Inc.</organization>
   49 |       <organizationUrl>https://www.spring.io</organizationUrl>
   50 |       <roles>
   51 |         <role>Project lead</role>
   52 |       </roles>
   53 |     </developer>
   54 |     <developer>
   55 |       <id>sgibb</id>
   56 |       <name>Spencer Gibb</name>
   57 |       <email>sgibb at pivotal.io</email>
   58 |       <organization>Pivotal Software, Inc.</organization>
   59 |       <organizationUrl>https://www.spring.io</organizationUrl>
   60 |       <roles>
   61 |         <role>Project lead</role>
   62 |       </roles>
   63 |     </developer>
   64 |   </developers>
   65 |   <scm>
   66 |     <connection>scm:git:git://github.com/spring-cloud/spring-cloud-release.git/spring-cloud-dependencies</connection>
   67 |     <developerConnection>scm:git:ssh://git@github.com/spring-cloud/spring-cloud-release.git/spring-cloud-dependencies</developerConnection>
   68 |     <url>https://github.com/spring-cloud/spring-cloud-release/spring-cloud-dependencies</url>
   69 |   </scm>
   70 |   <distributionManagement>
   71 |     <repository>
   72 |       <id>repo.spring.io</id>
   73 |       <name>Spring Release Repository</name>
   74 |       <url>https://repo.spring.io/libs-release-local</url>
   75 |     </repository>
   76 |     <snapshotRepository>
   77 |       <id>repo.spring.io</id>
   78 |       <name>Spring Snapshot Repository</name>
   79 |       <url>https://repo.spring.io/libs-snapshot-local</url>
   80 |     </snapshotRepository>
   81 |     <site>
   82 |       <id>spring-docs</id>
   83 |       <url>https://docs.spring.io/spring-cloud-release/reference/</url>
   84 |     </site>
   85 |     <downloadUrl>https://github.com/spring-cloud/spring-cloud-release</downloadUrl>
   86 |   </distributionManagement>
   87 |   <properties>
   88 |     <spring-cloud-circuitbreaker.version>3.2.0</spring-cloud-circuitbreaker.version>
   89 |     <spring-cloud-bus.version>4.2.0</spring-cloud-bus.version>
   90 |     <spring-cloud-consul.version>4.2.0</spring-cloud-consul.version>
   91 |     <spring-cloud-stream.version>4.2.0</spring-cloud-stream.version>
   92 |     <spring-cloud-contract.version>4.2.0</spring-cloud-contract.version>
   93 |     <spring-cloud-openfeign.version>4.2.0</spring-cloud-openfeign.version>
   94 |     <spring-cloud-config.version>4.2.0</spring-cloud-config.version>
   95 |     <spring-cloud-gateway.version>4.2.0</spring-cloud-gateway.version>
   96 |     <spring-cloud-task.version>3.2.0</spring-cloud-task.version>
   97 |     <spring-cloud-vault.version>4.2.0</spring-cloud-vault.version>
   98 |     <main.basedir>${basedir}/../..</main.basedir>
   99 |     <spring-cloud-netflix.version>4.2.0</spring-cloud-netflix.version>
  100 |     <spring-cloud-function.version>4.2.0</spring-cloud-function.version>
  101 |     <spring-cloud-build.version>4.2.0</spring-cloud-build.version>
  102 |     <spring-cloud-commons.version>4.2.0</spring-cloud-commons.version>
  103 |     <spring-cloud-kubernetes.version>3.2.0</spring-cloud-kubernetes.version>
  104 |     <spring-cloud-zookeeper.version>4.2.0</spring-cloud-zookeeper.version>
  105 |   </properties>
  106 |   <dependencyManagement>
  107 |     <dependencies>
  108 |       <dependency>
  109 |         <groupId>org.springframework.cloud</groupId>
  110 |         <artifactId>spring-cloud-commons-dependencies</artifactId>
  111 |         <version>${spring-cloud-commons.version}</version>
  112 |         <type>pom</type>
  113 |         <scope>import</scope>
  114 |       </dependency>
  115 |       <dependency>
  116 |         <groupId>org.springframework.cloud</groupId>
  117 |         <artifactId>spring-cloud-netflix-dependencies</artifactId>
  118 |         <version>${spring-cloud-netflix.version}</version>
  119 |         <type>pom</type>
  120 |         <scope>import</scope>
  121 |       </dependency>
  122 |       <dependency>
  123 |         <groupId>org.springframework.cloud</groupId>
  124 |         <artifactId>spring-cloud-stream-dependencies</artifactId>
  125 |         <version>${spring-cloud-stream.version}</version>
  126 |         <type>pom</type>
  127 |         <scope>import</scope>
  128 |       </dependency>
  129 |       <dependency>
  130 |         <groupId>org.springframework.cloud</groupId>
  131 |         <artifactId>spring-cloud-task-dependencies</artifactId>
  132 |         <version>${spring-cloud-task.version}</version>
  133 |         <type>pom</type>
  134 |         <scope>import</scope>
  135 |       </dependency>
  136 |       <dependency>
  137 |         <groupId>org.springframework.cloud</groupId>
  138 |         <artifactId>spring-cloud-circuitbreaker-dependencies</artifactId>
  139 |         <version>${spring-cloud-circuitbreaker.version}</version>
  140 |         <type>pom</type>
  141 |         <scope>import</scope>
  142 |       </dependency>
  143 |       <dependency>
  144 |         <groupId>org.springframework.cloud</groupId>
  145 |         <artifactId>spring-cloud-config-dependencies</artifactId>
  146 |         <version>${spring-cloud-config.version}</version>
  147 |         <type>pom</type>
  148 |         <scope>import</scope>
  149 |       </dependency>
  150 |       <dependency>
  151 |         <groupId>org.springframework.cloud</groupId>
  152 |         <artifactId>spring-cloud-function-dependencies</artifactId>
  153 |         <version>${spring-cloud-function.version}</version>
  154 |         <type>pom</type>
  155 |         <scope>import</scope>
  156 |       </dependency>
  157 |       <dependency>
  158 |         <groupId>org.springframework.cloud</groupId>
  159 |         <artifactId>spring-cloud-gateway-dependencies</artifactId>
  160 |         <version>${spring-cloud-gateway.version}</version>
  161 |         <type>pom</type>
  162 |         <scope>import</scope>
  163 |       </dependency>
  164 |       <dependency>
  165 |         <groupId>org.springframework.cloud</groupId>
  166 |         <artifactId>spring-cloud-consul-dependencies</artifactId>
  167 |         <version>${spring-cloud-consul.version}</version>
  168 |         <type>pom</type>
  169 |         <scope>import</scope>
  170 |       </dependency>
  171 |       <dependency>
  172 |         <groupId>org.springframework.cloud</groupId>
  173 |         <artifactId>spring-cloud-vault-dependencies</artifactId>
  174 |         <version>${spring-cloud-vault.version}</version>
  175 |         <type>pom</type>
  176 |         <scope>import</scope>
  177 |       </dependency>
  178 |       <dependency>
  179 |         <groupId>org.springframework.cloud</groupId>
  180 |         <artifactId>spring-cloud-zookeeper-dependencies</artifactId>
  181 |         <version>${spring-cloud-zookeeper.version}</version>
  182 |         <type>pom</type>
  183 |         <scope>import</scope>
  184 |       </dependency>
  185 |       <dependency>
  186 |         <groupId>org.springframework.cloud</groupId>
  187 |         <artifactId>spring-cloud-bus-dependencies</artifactId>
  188 |         <version>${spring-cloud-bus.version}</version>
  189 |         <type>pom</type>
  190 |         <scope>import</scope>
  191 |       </dependency>
  192 |       <dependency>
  193 |         <groupId>org.springframework.cloud</groupId>
  194 |         <artifactId>spring-cloud-contract-dependencies</artifactId>
  195 |         <version>${spring-cloud-contract.version}</version>
  196 |         <type>pom</type>
  197 |         <scope>import</scope>
  198 |       </dependency>
  199 |       <dependency>
  200 |         <groupId>org.springframework.cloud</groupId>
  201 |         <artifactId>spring-cloud-openfeign-dependencies</artifactId>
  202 |         <version>${spring-cloud-openfeign.version}</version>
  203 |         <type>pom</type>
  204 |         <scope>import</scope>
  205 |       </dependency>
  206 |       <dependency>
  207 |         <groupId>org.springframework.cloud</groupId>
  208 |         <artifactId>spring-cloud-kubernetes-dependencies</artifactId>
  209 |         <version>${spring-cloud-kubernetes.version}</version>
  210 |         <type>pom</type>
  211 |         <scope>import</scope>
  212 |       </dependency>
  213 |     </dependencies>
  214 |   </dependencyManagement>
  215 |   <profiles>
  216 |     <profile>
  217 |       <id>spring</id>
  218 |       <repositories>
  219 |         <repository>
  220 |           <snapshots>
  221 |             <enabled>true</enabled>
  222 |           </snapshots>
  223 |           <id>spring-snapshots</id>
  224 |           <name>Spring Snapshots</name>
  225 |           <url>https://repo.spring.io/libs-snapshot-local</url>
  226 |         </repository>
  227 |         <repository>
  228 |           <snapshots>
  229 |             <enabled>false</enabled>
  230 |           </snapshots>
  231 |           <id>spring-milestones</id>
  232 |           <name>Spring Milestones</name>
  233 |           <url>https://repo.spring.io/libs-milestone-local</url>
  234 |         </repository>
  235 |         <repository>
  236 |           <snapshots>
  237 |             <enabled>false</enabled>
  238 |           </snapshots>
  239 |           <id>spring-releases</id>
  240 |           <name>Spring Releases</name>
  241 |           <url>https://repo.spring.io/release</url>
  242 |         </repository>
  243 |       </repositories>
  244 |       <pluginRepositories>
  245 |         <pluginRepository>
  246 |           <snapshots>
  247 |             <enabled>true</enabled>
  248 |           </snapshots>
  249 |           <id>spring-snapshots</id>
  250 |           <name>Spring Snapshots</name>
  251 |           <url>https://repo.spring.io/libs-snapshot-local</url>
  252 |         </pluginRepository>
  253 |         <pluginRepository>
  254 |           <snapshots>
  255 |             <enabled>false</enabled>
  256 |           </snapshots>
  257 |           <id>spring-milestones</id>
  258 |           <name>Spring Milestones</name>
  259 |           <url>https://repo.spring.io/libs-milestone-local</url>
  260 |         </pluginRepository>
  261 |       </pluginRepositories>
  262 |     </profile>
  263 |   </profiles>
  264 | </project>
```
