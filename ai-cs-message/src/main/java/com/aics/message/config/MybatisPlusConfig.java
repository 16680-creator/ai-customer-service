package com.aics.message.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 配置：分页插件 + 时间字段自动填充
 * <p>
 * 所属模块：ai-cs-message。
 * 职责：为 MyBatis-Plus 注册全局拦截器与元对象填充器，统一处理分页与 createTime/updateTime 自动注入，
 * 避免业务代码里手动 set 时间字段或拼分页 SQL。
 * 技术要点：
 * <ul>
 *     <li>{@link MybatisPlusInterceptor} 内置 {@link PaginationInnerInterceptor}，按 MySQL 方言自动改写分页 SQL；</li>
 *     <li>{@link MetaObjectHandler} 在 insert/update 时自动填充时间字段，需与实体上的
 *     {@code @TableField(fill = FieldFill.INSERT/INSERT_UPDATE)} 配合。</li>
 * </ul>
 * </p>
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        // 注册分页内拦截器，指定 MySQL 方言以生成正确的 LIMIT 语句
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * 元对象填充处理器：在插入与更新时自动维护时间字段
     * <p>
     * 触发条件需实体字段标注 {@code @TableField(fill = FieldFill.INSERT/INSERT_UPDATE)}，
     * 否则 strictInsertFill/strictUpdateFill 不会生效。
     * </p>
     *
     * @return MyBatis-Plus 元对象填充器
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                // 插入时同时填充 createTime 与 updateTime
                this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                // 更新时仅刷新 updateTime，createTime 不变
                this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}