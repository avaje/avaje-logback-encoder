package io.avaje.logback.encoder;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import io.avaje.json.simple.SimpleMapper;
import io.avaje.logback.encoder.abbreviator.TrimPackageAbbreviator;
import org.junit.jupiter.api.Test;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static ch.qos.logback.classic.Level.INFO;
import static org.assertj.core.api.Assertions.assertThat;

class JsonEncoderTest {

    private final String fqcn = "org.example.Foo";

    private ILoggingEvent createLogEvent() {
        return createLogEvent(null);
    }

    private ILoggingEvent createLogEvent(Throwable throwable) {
        Logger logger = (Logger)LoggerFactory.getLogger(fqcn);
        return new LoggingEvent(fqcn, logger, INFO, "Hi", throwable, null);
    }

    @Test
    void encode() {
        ILoggingEvent event = createLogEvent();

        JsonEncoder encoder = new JsonEncoder();
        encoder.start();
        byte[] bytes = encoder.encode(event);

        SimpleMapper simpleMapper = SimpleMapper.builder().build();
        Map<String, Object> asMap = simpleMapper.map().fromJson(bytes);

        assertThat((String)asMap.get("component")).isNull();
        assertThat((String)asMap.get("env")).isNull();
        assertThat((String)asMap.get("timestamp")).isNotNull();
        assertThat((String)asMap.get("message")).isEqualTo("Hi");
        assertThat((String)asMap.get("level")).isEqualTo("INFO");
        assertThat((String)asMap.get("thread")).isEqualTo("main");
        assertThat((String)asMap.get("logger")).isEqualTo("org.example.Foo");
    }

    @Test
    void encode_component() {
        JsonEncoder encoder = new JsonEncoder();
        encoder.setComponent("my-component");
        encoder.setEnvironment("dev");
        encoder.start();

        byte[] bytes = encoder.encode(createLogEvent());

        SimpleMapper simpleMapper = SimpleMapper.builder().build();
        Map<String, Object> asMap = simpleMapper.map().fromJson(bytes);

        assertThat((String)asMap.get("component")).isEqualTo("my-component");
        assertThat((String)asMap.get("env")).isEqualTo("dev");
        assertThat((String)asMap.get("timestamp")).isNotNull();
        assertThat((String)asMap.get("message")).isEqualTo("Hi");
        assertThat((String)asMap.get("level")).isEqualTo("INFO");
        assertThat((String)asMap.get("thread")).isEqualTo("main");
        assertThat((String)asMap.get("logger")).isEqualTo("org.example.Foo");
    }

    @Test
    void throwable_usingDefault() {
        JsonEncoder encoder = new JsonEncoder();
        encoder.start();

        byte[] bytes = encoder.encode(createLogEvent(createThrowable()));
        SimpleMapper simpleMapper = SimpleMapper.builder().build();
        Map<String, Object> asMap = simpleMapper.map().fromJson(bytes);

        assertThat((String)asMap.get("exception")).startsWith("java.lang.NullPointerException: ");
    }

    @Test
    void throwable_usingConverter() {
        final TrimPackageAbbreviator trimPackages = new TrimPackageAbbreviator();
        trimPackages.setTargetLength(10);

        final ShortenedThrowableConverter converter = new ShortenedThrowableConverter();
        converter.setMaxDepthPerThrowable(3);
        converter.setClassNameAbbreviator(trimPackages);

        JsonEncoder encoder = new JsonEncoder();
        encoder.setThrowableConverter(converter);
        encoder.start();

        byte[] bytes = encoder.encode(createLogEvent(createThrowable()));
        SimpleMapper simpleMapper = SimpleMapper.builder().build();
        Map<String, Object> asMap = simpleMapper.map().fromJson(bytes);

        assertThat((String)asMap.get("exception")).startsWith("j.l.NullPointerException: ");
    }

    Throwable createThrowable() {
        try {
            System.getProperty("doNotExist").toUpperCase();
            return null;
        } catch (Throwable e) {
            return e;
        }
    }
}