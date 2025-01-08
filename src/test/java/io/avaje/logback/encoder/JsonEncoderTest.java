package io.avaje.logback.encoder;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import io.avaje.json.simple.SimpleMapper;
import org.junit.jupiter.api.Test;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static ch.qos.logback.classic.Level.INFO;
import static org.assertj.core.api.Assertions.assertThat;

class JsonEncoderTest {

    @Test
    void encode() {
        String fqcn = "org.example.Foo";
        Logger logger = (Logger)LoggerFactory.getLogger(fqcn);

        ILoggingEvent event = new LoggingEvent(fqcn, logger, INFO, "Hi", null, null);

        JsonEncoder encoder = new JsonEncoder();
        encoder.start();
        byte[] bytes = encoder.encode(event);

        SimpleMapper simpleMapper = SimpleMapper.builder().build();
        Map<String, Object> asMap = simpleMapper.map().fromJson(bytes);

        assertThat((String)asMap.get("@timestamp")).isNotNull();
        assertThat((String)asMap.get("message")).isEqualTo("Hi");
        assertThat((String)asMap.get("level")).isEqualTo("INFO");
        assertThat((String)asMap.get("thread")).isEqualTo("main");
        assertThat((String)asMap.get("logger")).isEqualTo("org.example.Foo");
    }
}