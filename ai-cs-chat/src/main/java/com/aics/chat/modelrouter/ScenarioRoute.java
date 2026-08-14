package com.aics.chat.modelrouter;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ScenarioRoute {
    private String primary;
    // 设计要点：fallbacks 默认空列表而非 null——调用方无需判空，省略 fallback 与显式空链语义一致
    private List<String> fallbacks = new ArrayList<>();
}
