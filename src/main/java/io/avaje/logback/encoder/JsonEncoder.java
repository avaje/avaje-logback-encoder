package io.avaje.logback.encoder;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.EncoderBase;
import io.avaje.json.PropertyNames;
import io.avaje.json.simple.SimpleMapper;
import io.avaje.json.stream.JsonStream;

public final class JsonEncoder extends EncoderBase<ILoggingEvent> {

  private static final byte[] EMPTY_BYTES = {};
  private final JsonStream json;
  private final Map<String, String> customFieldsMap = new HashMap<>();
  private final PropertyNames properties;
  private ThrowableHandlingConverter throwableConverter = new ShortenedThrowableConverter();

  private DateTimeFormatter formatter;
  private TimeZone timeZone = TimeZone.getDefault();
  /** Null implies default of ISO_OFFSET_DATE_TIME */
  private String timestampPattern;
  private int fieldExtra;
  private String component;
  private String environment;

  public JsonEncoder() {
    this.json = JsonStream.builder().build();
    this.properties = json.properties("component", "env", "timestamp", "level", "logger", "message", "thread", "exception");
  }

  @Override
  public void start() {
    formatter = TimeZoneUtils.formatter(timestampPattern, timeZone.toZoneId());
    fieldExtra =
        customFieldsMap.entrySet().stream()
            .mapToInt(e -> e.getKey().length() + e.getValue().length())
            .sum();
    super.start();
    throwableConverter.start();
  }

  @Override
  public void stop() {
    super.stop();
    throwableConverter.stop();
  }

  @Override
  public byte[] headerBytes() {
    return EMPTY_BYTES;
  }

  @Override
  public byte[] footerBytes() {
    return EMPTY_BYTES;
  }

  @Override
  public byte[] encode(ILoggingEvent event) {
    final String stackTraceBody = throwableConverter.convert(event);
    final int extra = stackTraceBody.isEmpty() ? 0 : 20 + stackTraceBody.length();

    final var threadName = event.getThreadName();
    final var message = event.getFormattedMessage();
    final var loggerName = event.getLoggerName();
    final int bufferSize = 100 + extra + fieldExtra + message.length() + threadName.length() + loggerName.length();
    final var outputStream = new ByteArrayOutputStream(bufferSize);

    try (var writer = json.writer(outputStream)) {
      writer.beginObject(properties);
      if (component != null) {
        writer.name(0);
        writer.value(component);
      }
      if (environment != null) {
        writer.name(1);
        writer.value(environment);
      }
      writer.name(2);
      writer.value(formatter.format(Instant.ofEpochMilli(event.getTimeStamp())));
      writer.name(3);
      writer.value(event.getLevel().toString());
      writer.name(4);
      writer.value(loggerName);
      writer.name(5);
      writer.value(message);
      writer.name(6);
      writer.value(threadName);
      if (!stackTraceBody.isEmpty()) {
        writer.name(7);
        writer.value(stackTraceBody);
      }
      customFieldsMap.forEach((k, v) -> {
        writer.name(k);
        writer.rawValue(v);
      });
      event.getMDCPropertyMap().forEach((k, v) -> {
        writer.name(k);
        writer.value(v);
      });
      writer.endObject();
      writer.writeNewLine();
    }
    return outputStream.toByteArray();
  }

  public void setComponent(String component) {
    this.component = component;
  }

  public void setEnvironment(String environment) {
    this.environment = environment;
  }

  public void setThrowableConverter(ThrowableHandlingConverter throwableConverter) {
    this.throwableConverter = throwableConverter;
  }

  public void setCustomFields(String customFields) {
    if (customFields == null || customFields.isBlank()) {
      return;
    }
    var mapper = SimpleMapper.builder().jsonStream(json).build();
    mapper.map().fromJson(customFields).forEach((k, v) -> customFieldsMap.put(k, mapper.toJson(v)));
  }

  public void setTimestampPattern(String pattern) {
    this.timestampPattern = pattern;
  }

  public void setTimeZone(String timeZone) {
    this.timeZone = TimeZoneUtils.parseTimeZone(timeZone);
  }
}
