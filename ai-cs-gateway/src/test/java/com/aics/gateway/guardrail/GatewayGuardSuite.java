package com.aics.gateway.guardrail;

import io.cucumber.junit.platform.engine.Constants;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * 3.2 网关安全 —— Cucumber BDD Suite（features/gateway/ 2 个 Feature）。
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/gateway")
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, value = "com.aics.gateway.guardrail")
@ConfigurationParameter(key = Constants.PLUGIN_PROPERTY_NAME, value = "pretty")
public class GatewayGuardSuite {
}
