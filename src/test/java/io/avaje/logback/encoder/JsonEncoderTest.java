package io.avaje.logback.encoder;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import io.avaje.json.mapper.JsonMapper;
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

        JsonMapper mapper = JsonMapper.builder().build();
        Map<String, Object> asMap = mapper.map().fromJson(bytes);

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

        JsonMapper mapper = JsonMapper.builder().build();
        Map<String, Object> asMap = mapper.map().fromJson(bytes);

        assertThat((String)asMap.get("component")).isEqualTo("my-component");
        assertThat((String)asMap.get("env")).isEqualTo("dev");
        assertThat((String)asMap.get("timestamp")).isNotNull();
        assertThat((String)asMap.get("message")).isEqualTo("Hi");
        assertThat((String)asMap.get("level")).isEqualTo("INFO");
        assertThat((String)asMap.get("thread")).isEqualTo("main");
        assertThat((String)asMap.get("logger")).isEqualTo("org.example.Foo");
    }

    @Test
    void customFieldsEval() {
        System.setProperty("some.custom.property", "Hi!");
        JsonEncoder encoder = new JsonEncoder();
        encoder.setCustomFields("{\"my-custom\":\"${some.custom.property}\", \"other\": \"myLiteral\", \"more\": 12}");
        encoder.start();

        byte[] bytes = encoder.encode(createLogEvent(createThrowable()));
        JsonMapper simpleMapper = JsonMapper.builder().build();
        Map<String, Object> asMap = simpleMapper.map().fromJson(bytes);

        assertThat((String)asMap.get("my-custom")).isEqualTo("Hi!");
        assertThat((String)asMap.get("other")).isEqualTo("myLiteral");
        assertThat((Long)asMap.get("more")).isEqualTo(12L);
    }

  @Test
  void throwable_usingDefault() {
    JsonEncoder encoder = new JsonEncoder();
    encoder.start();

    byte[] bytes = encoder.encode(createLogEvent(createThrowable()));
    JsonMapper simpleMapper = JsonMapper.builder().build();
    Map<String, Object> asMap = simpleMapper.map().fromJson(bytes);

    assertThat((String)asMap.get("stacktrace")).startsWith("java.lang.NullPointerException: ");
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
        encoder.setIncludeStackHash(false);
        encoder.start();

        byte[] bytes = encoder.encode(createLogEvent(createThrowable()));
        JsonMapper simpleMapper = JsonMapper.builder().build();
        Map<String, Object> asMap = simpleMapper.map().fromJson(bytes);

        assertThat((String)asMap.get("stacktrace")).startsWith("j.l.NullPointerException: ");
        assertThat(asMap).doesNotContainKey("stackhash");
    }

    @Test
    void throwable_usingConverter_includeStackHash() {
        final TrimPackageAbbreviator trimPackages = new TrimPackageAbbreviator();
        trimPackages.setTargetLength(10);

        final ShortenedThrowableConverter converter = new ShortenedThrowableConverter();
        converter.setMaxDepthPerThrowable(3);
        converter.setClassNameAbbreviator(trimPackages);

        JsonEncoder encoder = new JsonEncoder();
        encoder.setThrowableConverter(converter);
        encoder.setIncludeStackHash(true);
        encoder.start();

        byte[] bytes = encoder.encode(createLogEvent(createThrowable()));
        JsonMapper simpleMapper = JsonMapper.builder().build();
        Map<String, Object> asMap = simpleMapper.map().fromJson(bytes);

        assertThat((String)asMap.get("stacktrace")).startsWith("j.l.NullPointerException: ");
        assertThat(asMap.get("stackhash")).isNotNull();
    }

    @Test
    void awsAppender() {
      StdOutAppender appender = new StdOutAppender();
      appender.setComponent("my-other");
      appender.setEnvironment("localdev");
      appender.start();

      appender.append(createLogEvent());
      appender.append(createLogEvent(createThrowable()));
      appender.append(createLogEvent());
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
