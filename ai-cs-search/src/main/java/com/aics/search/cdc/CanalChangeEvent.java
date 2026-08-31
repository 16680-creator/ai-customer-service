package com.aics.search.cdc;

import lombok.Data;

import java.util.List;
import java.util.Map;

/** Canal RocketMQ adapter 的通用 JSON 事件信封（兼容 INSERT/UPDATE/DELETE）。 */
@Data
public class CanalChangeEvent {
    private String database;
    private String table;
    private String type;
    private List<Map<String, Object>> data;
}
