<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-dependencies:2026.0.0 | path: spring-cloud-dependencies-2026.0.0.pom -->
<!-- 正文引用：[[源码:spring-cloud-dependencies-2026.0.0.pom#L起-L止]] ；代码块头部：(src: spring-cloud-dependencies-2026.0.0.pom#L起-L止) -->

# spring-cloud-dependencies-2026.0.0.pom

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-dependencies:2026.0.0` |
| 路径 | `spring-cloud-dependencies-2026.0.0.pom` |
| 行数 | 231 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-dependencies/2026.0.0/spring-cloud-dependencies-2026.0.0.pom |
| 抓取时间 | 2026-11-26T00:12:00.000Z |

```java
    1 | <?xml version="1.0" encoding="UTF-8"?>
    2 | <project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
    3 |     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    4 |   <modelVersion>4.0.0</modelVersion>
    5 |   <parent>
    6 |     <groupId>org.springframework.cloud</groupId>
    7 |     <artifactId>spring-cloud-dependencies-parent</artifactId>
    8 |     <version>5.1.0</version>
    9 |     <relativePath></relativePath>
   10 |   </parent>
   11 |   <groupId>org.springframework.cloud</groupId>
   12 |   <artifactId>spring-cloud-dependencies</artifactId>
   13 |   <version>2026.0.0</version>
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
   88 |     <spring-cloud-circuitbreaker.version>5.1.0</spring-cloud-circuitbreaker.version>
   89 |     <spring-cloud-consul.version>5.1.0</spring-cloud-consul.version>
   90 |     <spring-cloud-stream.version>5.1.0</spring-cloud-stream.version>
   91 |     <spring-cloud-openfeign.version>5.1.0</spring-cloud-openfeign.version>
   92 |     <spring-boot.version>4.2.0</spring-boot.version>
   93 |     <spring-cloud-config.version>5.1.0</spring-cloud-config.version>
   94 |     <spring-cloud-gateway.version>5.1.0</spring-cloud-gateway.version>
   95 |     <spring-cloud-task.version>5.1.0</spring-cloud-task.version>
   96 |     <spring-cloud-vault.version>5.1.0</spring-cloud-vault.version>
   97 |     <main.basedir>${basedir}/../..</main.basedir>
   98 |     <spring-cloud-netflix.version>5.1.0</spring-cloud-netflix.version>
   99 |     <spring-cloud-function.version>5.1.0</spring-cloud-function.version>
  100 |     <spring-cloud-build.version>5.1.0</spring-cloud-build.version>
  101 |     <spring-cloud-commons.version>5.1.0</spring-cloud-commons.version>
  102 |     <spring-cloud-kubernetes.version>5.1.0</spring-cloud-kubernetes.version>
  103 |     <spring-cloud-zookeeper.version>5.1.0</spring-cloud-zookeeper.version>
  104 |   </properties>
  105 |   <dependencyManagement>
  106 |     <dependencies>
  107 |       <dependency>
  108 |         <groupId>org.springframework.cloud</groupId>
  109 |         <artifactId>spring-cloud-commons-dependencies</artifactId>
  110 |         <version>${spring-cloud-commons.version}</version>
  111 |         <type>pom</type>
  112 |         <scope>import</scope>
  113 |       </dependency>
  114 |       <dependency>
  115 |         <groupId>org.springframework.cloud</groupId>
  116 |         <artifactId>spring-cloud-netflix-dependencies</artifactId>
  117 |         <version>${spring-cloud-netflix.version}</version>
  118 |         <type>pom</type>
  119 |         <scope>import</scope>
  120 |       </dependency>
  121 |       <dependency>
  122 |         <groupId>org.springframework.cloud</groupId>
  123 |         <artifactId>spring-cloud-stream-dependencies</artifactId>
  124 |         <version>${spring-cloud-stream.version}</version>
  125 |         <type>pom</type>
  126 |         <scope>import</scope>
  127 |       </dependency>
  128 |       <dependency>
  129 |         <groupId>org.springframework.cloud</groupId>
  130 |         <artifactId>spring-cloud-task-dependencies</artifactId>
  131 |         <version>${spring-cloud-task.version}</version>
  132 |         <type>pom</type>
  133 |         <scope>import</scope>
  134 |       </dependency>
  135 |       <dependency>
  136 |         <groupId>org.springframework.cloud</groupId>
  137 |         <artifactId>spring-cloud-circuitbreaker-dependencies</artifactId>
  138 |         <version>${spring-cloud-circuitbreaker.version}</version>
  139 |         <type>pom</type>
  140 |         <scope>import</scope>
  141 |       </dependency>
  142 |       <dependency>
  143 |         <groupId>org.springframework.cloud</groupId>
  144 |         <artifactId>spring-cloud-config-dependencies</artifactId>
  145 |         <version>${spring-cloud-config.version}</version>
  146 |         <type>pom</type>
  147 |         <scope>import</scope>
  148 |       </dependency>
  149 |       <dependency>
  150 |         <groupId>org.springframework.cloud</groupId>
  151 |         <artifactId>spring-cloud-function-dependencies</artifactId>
  152 |         <version>${spring-cloud-function.version}</version>
  153 |         <type>pom</type>
  154 |         <scope>import</scope>
  155 |       </dependency>
  156 |       <dependency>
  157 |         <groupId>org.springframework.cloud</groupId>
  158 |         <artifactId>spring-cloud-gateway-dependencies</artifactId>
  159 |         <version>${spring-cloud-gateway.version}</version>
  160 |         <type>pom</type>
  161 |         <scope>import</scope>
  162 |       </dependency>
  163 |       <dependency>
  164 |         <groupId>org.springframework.cloud</groupId>
  165 |         <artifactId>spring-cloud-consul-dependencies</artifactId>
  166 |         <version>${spring-cloud-consul.version}</version>
  167 |         <type>pom</type>
  168 |         <scope>import</scope>
  169 |       </dependency>
  170 |       <dependency>
  171 |         <groupId>org.springframework.cloud</groupId>
  172 |         <artifactId>spring-cloud-vault-dependencies</artifactId>
  173 |         <version>${spring-cloud-vault.version}</version>
  174 |         <type>pom</type>
  175 |         <scope>import</scope>
  176 |       </dependency>
  177 |       <dependency>
  178 |         <groupId>org.springframework.cloud</groupId>
  179 |         <artifactId>spring-cloud-zookeeper-dependencies</artifactId>
  180 |         <version>${spring-cloud-zookeeper.version}</version>
  181 |         <type>pom</type>
  182 |         <scope>import</scope>
  183 |       </dependency>
  184 |       <dependency>
  185 |         <groupId>org.springframework.cloud</groupId>
  186 |         <artifactId>spring-cloud-openfeign-dependencies</artifactId>
  187 |         <version>${spring-cloud-openfeign.version}</version>
  188 |         <type>pom</type>
  189 |         <scope>import</scope>
  190 |       </dependency>
  191 |       <dependency>
  192 |         <groupId>org.springframework.cloud</groupId>
  193 |         <artifactId>spring-cloud-kubernetes-dependencies</artifactId>
  194 |         <version>${spring-cloud-kubernetes.version}</version>
  195 |         <type>pom</type>
  196 |         <scope>import</scope>
  197 |       </dependency>
  198 |     </dependencies>
  199 |   </dependencyManagement>
  200 |   <profiles>
  201 |     <profile>
  202 |       <id>spring</id>
  203 |       <repositories>
  204 |         <repository>
  205 |           <snapshots>
  206 |             <enabled>true</enabled>
  207 |           </snapshots>
  208 |           <id>spring-snapshots</id>
  209 |           <name>Spring Snapshots</name>
  210 |           <url>https://repo.spring.io/libs-snapshot-local</url>
  211 |         </repository>
  212 |         <repository>
  213 |           <snapshots>
  214 |             <enabled>false</enabled>
  215 |           </snapshots>
  216 |           <id>spring-milestones</id>
  217 |           <name>Spring Milestones</name>
  218 |           <url>https://repo.spring.io/libs-milestone-local</url>
  219 |         </repository>
  220 |         <repository>
  221 |           <snapshots>
  222 |             <enabled>false</enabled>
  223 |           </snapshots>
  224 |           <id>spring-releases</id>
  225 |           <name>Spring Releases</name>
  226 |           <url>https://repo.spring.io/release</url>
  227 |         </repository>
  228 |       </repositories>
  229 |     </profile>
  230 |   </profiles>
  231 | </project>
```
