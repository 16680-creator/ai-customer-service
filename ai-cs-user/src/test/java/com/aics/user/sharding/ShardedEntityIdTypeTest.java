package com.aics.user.sharding;

import com.aics.user.entity.User;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * 分片表主键策略守护测试（03-P6 分布式 ID 规范）。
 *
 * <p>规范：分片表禁止数据库自增主键（IdType.AUTO）——自增唯一性只在单库内成立，
 * 与分片路由（ID 后四位取模）和扩容合并天然冲突。用反射扫描实体包把规则锁死，
 * 防止后续新实体或重构时配回 AUTO。</p>
 *
 * @see <a href="learning-docs/03-数据库与ORM/04-分布式ID规范.md">分布式 ID 规范</a>
 */
class ShardedEntityIdTypeTest {

    @Test
    @DisplayName("分片表实体（user）必须使用应用侧生成 ID（ASSIGN_ID），禁止 AUTO")
    void shardedEntityMustNotUseAutoId() throws Exception {
        assertNotEquals(IdType.AUTO, idTypeOf(User.class),
                "User 表按 ID 后四位分片，主键必须由应用侧（雪花）生成，禁止数据库自增");
        assertEquals(IdType.ASSIGN_ID, idTypeOf(User.class),
                "User 主键应为 MyBatis-Plus 内置雪花（ASSIGN_ID）");
    }

    @Test
    @DisplayName("实体包全量扫描 - 所有实体主键策略必须显式声明且不为 AUTO")
    void allEntitiesMustDeclareExplicitNonAutoIdType() throws Exception {
        List<String> violations = new ArrayList<>();
        for (Class<?> clazz : scanEntityClasses()) {
            Field field = findIdField(clazz);
            if (field == null) {
                violations.add(clazz.getSimpleName() + ": 未找到 @TableId 字段");
                continue;
            }
            TableId tableId = field.getAnnotation(TableId.class);
            if (tableId.type() == IdType.AUTO) {
                violations.add(clazz.getSimpleName() + ": 分库分表工程内禁止 IdType.AUTO（见分布式 ID 规范）");
            }
        }
        assertEquals(List.of(), violations, "存在违反分布式 ID 规范的实体");
    }

    private IdType idTypeOf(Class<?> clazz) throws Exception {
        Field field = findIdField(clazz);
        return field == null ? null : field.getAnnotation(TableId.class).type();
    }

    private Field findIdField(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(TableId.class)) {
                return field;
            }
        }
        return null;
    }

    /**
     * 扫描 user 模块 entity 包（编译产物 classpath 扫描，避免引入额外依赖）。
     */
    private List<Class<?>> scanEntityClasses() throws Exception {
        List<Class<?>> result = new ArrayList<>();
        String packageName = "com.aics.user.entity";
        String path = packageName.replace('.', '/');
        var resources = Thread.currentThread().getContextClassLoader().getResources(path);
        while (resources.hasMoreElements()) {
            var url = resources.nextElement();
            if (!"file".equals(url.getProtocol())) {
                continue;
            }
            var dir = new java.io.File(url.toURI());
            var files = dir.listFiles((d, name) -> name.endsWith(".class"));
            if (files == null) {
                continue;
            }
            for (var file : files) {
                String className = packageName + '.' + file.getName().replace(".class", "");
                result.add(Class.forName(className));
            }
        }
        result.sort((a, b) -> a.getSimpleName().compareTo(b.getSimpleName()));
        return result;
    }
}
