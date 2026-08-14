package com.aics.knowledge.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 配置
 *
 * <p>职责：注册分页拦截器与元对象自动填充处理器，
 * 供 {@link com.aics.knowledge.mapper.KnowledgeMapper} 及其 Service 使用。</p>
 *
 * <p>技术要点：</p>
 * <ul>
 *   <li>分页插件：自动改写 SQL 添加 LIMIT，配合 Page 参数实现物理分页</li>
 *   <li>自动填充：insert 时填充 create_time/update_time，update 时刷新 update_time，
 *       避免业务代码手动维护时间字段</li>
 * </ul>
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * MyBatis-Plus 拦截器链：注册分页内拦截器
     *
     * @return 配置好分页插件的拦截器实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 指定数据库类型为 MySQL，分页 SQL 方言按 MySQL 语法生成
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * 元对象自动填充处理器（create_time / update_time）
     *
     * <p>配合实体类 {@link com.aics.knowledge.entity.KnowledgeDocument} 上的
     * @TableField(fill = FieldFill.INSERT / INSERT_UPDATE) 注解生效。</p>
     *
     * @return 自动填充处理器
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            /** 插入时同时填充 create_time 与 update_time */
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }

            /** 更新时仅刷新 update_time */
            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}