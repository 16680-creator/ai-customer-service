@echo off
cd /d D:\Projects\Persion\ai-customer-service
mvn.cmd -pl ai-cs-order spring-boot:run -DskipTests > tools\logs\ai-cs-order.springboot.out.log 2> tools\logs\ai-cs-order.springboot.err.log