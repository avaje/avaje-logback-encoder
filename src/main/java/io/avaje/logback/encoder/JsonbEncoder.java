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
import io.avaje.jsonb.Jsonb;
import io.avaje.jsonb.Types;
import io.avaje.jsonb.spi.PropertyNames;
import io.avaje.jsonb.stream.JsonStream;
import io.avaje.logback.encoder.abbreviator.TrimPackageAbbreviator;

public final class JsonbEncoder extends EncoderBase<ILoggingEvent> {

  private static final byte[] EMPTY_BYTES = {};
  private final JsonStream json;
  private final Map<String, String> customFieldsMap = new HashMap<>();
  private PropertyNames properties;
  private DateTimeFormatter formatter;

  private final ThrowableHandlingConverter throwableConverter;

  private TimeZone timeZone = TimeZone.getDefault();

  private String timestampPattern;

  private int fieldExtra;

  public JsonbEncoder() {
    this.json = JsonStream.builder().build();

    final var converter = new ShortenedThrowableConverter();
    converter.setMaxDepthPerThrowable(3);

    final var de = new TrimPackageAbbreviator();
    de.setTargetLength(20);
    converter.setClassNameAbbreviator(de);
    converter.setRootCauseFirst(true);
    throwableConverter = converter;
  }

  @Override
  public void start() {
    this.properties =
        json.properties("@timestamp", "level", "logger", "message", "thread", "stack_trace");
    formatter = TimeZoneUtils.getFormatter(timestampPattern, timeZone.toZoneId());
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
    final int bufferSize =
        100 + extra + fieldExtra + message.length() + threadName.length() + loggerName.length();
    final var outputStream = new ByteArrayOutputStream(bufferSize);

    try (var writer = json.writer(outputStream)) {
      writer.beginObject(properties);
      writer.name(0);
      writer.rawValue(formatter.format(Instant.ofEpochMilli(event.getTimeStamp())));
      writer.name(1);
      writer.rawValue(event.getLevel().toString());
      writer.name(2);
      writer.value(loggerName);
      writer.name(3);
      writer.value(message);
      writer.name(4);
      writer.value(threadName);

      if (!stackTraceBody.isEmpty()) {
        writer.name(5);
        writer.value(stackTraceBody);
      }
      customFieldsMap.forEach(
          (k, v) -> {
            writer.name(k);
            writer.value(v);
          });
      event
          .getMDCPropertyMap()
          .forEach(
              (k, v) -> {
                writer.name(k);
                writer.value(v);
              });
      writer.endObject();
      writer.writeNewLine();
    }

    return outputStream.toByteArray();
  }

  public void setCustomFields(String customFields) {
    if (customFields == null || customFields.isBlank()) {
      return;
    }
    var jsonb = Jsonb.builder().adapter(json).build();
    jsonb
        .<Map<String, Object>>type(Types.mapOf(Object.class))
        .fromJson(customFields)
        .forEach((k, v) -> customFieldsMap.put(k, jsonb.toJson(v)));
  }

  public void setTimestampPattern(String pattern) {
    this.timestampPattern = pattern;
  }

  public void setTimeZone(String timeZone) {

    this.timeZone = TimeZoneUtils.parseTimeZone(timeZone);
  }
}
