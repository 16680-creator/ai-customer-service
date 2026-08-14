package com.aics.chat.security.guardrail;

import io.cucumber.junit.platform.engine.Constants;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * 3.2 AI 安全网关与 Guardrails —— Cucumber BDD Suite（features/security/ 7 个 Feature）。
 *
 * <p>mvn test 时经 junit-platform-suite 引擎自动执行，场景即验收标准。</p>
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/security")
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, value = "com.aics.chat.security.guardrail")
@ConfigurationParameter(key = Constants.PLUGIN_PROPERTY_NAME, value = "pretty")
public class SecurityGuardSuite {
}
