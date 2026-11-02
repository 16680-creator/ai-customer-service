"""
可观测性集成（日志、指标、追踪）

学习章节：22-Python速成/25-可观测性实战-日志、监控、链路追踪.md
"""
import json
import logging
from datetime import datetime

from opentelemetry import trace
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from opentelemetry.exporter.jaeger.thrift import JaegerExporter

from app.config import get_settings


# ============== 结构化日志 ==============
class JSONFormatter(logging.Formatter):
    """
    JSON 日志格式化（对标 Spring Boot 的结构化日志）

    Java 等价:
    <appender name="json" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder" />
    </appender>
    """

    def format(self, record: logging.LogRecord) -> str:
        log_data = {
            "timestamp": datetime.utcnow().isoformat(),
            "level": record.levelname,
            "logger": record.name,
            "message": record.getMessage(),
            "module": record.module,
            "lineno": record.lineno,
        }

        # 附加 extra 字段
        if hasattr(record, "__dict__"):
            for key, value in record.__dict__.items():
                if key not in [
                    "name",
                    "msg",
                    "args",
                    "created",
                    "filename",
                    "funcName",
                    "levelname",
                    "levelno",
                    "lineno",
                    "module",
                    "msecs",
                    "message",
                    "pathname",
                    "process",
                    "processName",
                    "relativeCreated",
                    "thread",
                    "threadName",
                    "exc_info",
                    "exc_text",
                    "stack_info",
                ]:
                    log_data[key] = value

        return json.dumps(log_data, ensure_ascii=False, default=str)


def setup_logging(level: str = "INFO") -> None:
    """
    配置日志系统

    对标 Spring Boot 的 logging configuration
    """
    settings = get_settings()

    logger = logging.getLogger()
    logger.setLevel(level)

    handler = logging.StreamHandler()
    handler.setFormatter(JSONFormatter())
    logger.addHandler(handler)


# ============== OpenTelemetry 链路追踪 ==============
def init_tracing() -> None:
    """
    初始化分布式链路追踪

    对标 Spring Cloud Sleuth + Zipkin/Jaeger

    Java 等价:
    spring.zipkin.base-url=http://jaeger:9411
    spring.sleuth.sampler.probability=1.0
    """
    settings = get_settings()

    jaeger_exporter = JaegerExporter(
        agent_host_name=settings.jaeger_host,
        agent_port=settings.jaeger_port,
    )

    trace_provider = TracerProvider()
    trace_provider.add_span_processor(BatchSpanProcessor(jaeger_exporter))
    trace.set_tracer_provider(trace_provider)
