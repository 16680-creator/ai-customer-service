package com.aics.chat.modelrouter;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ScenarioRoute {
    private String primary;
    private List<String> fallbacks = new ArrayList<>();
}
