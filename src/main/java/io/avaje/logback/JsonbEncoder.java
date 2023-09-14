package io.avaje.logback;

import java.io.ByteArrayOutputStream;
import java.time.Instant;

import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.EncoderBase;
import io.avaje.jsonb.JsonWriter;
import io.avaje.jsonb.spi.PropertyNames;
import io.avaje.jsonb.stream.JsonStream;
import io.avaje.logback.abbreviator.TrimPackageAbbreviator;

public final class JsonbEncoder extends EncoderBase<ILoggingEvent> {

  private static final byte[] EMPTY_BYTES = {};
  private final PropertyNames properties;
  private final JsonStream json;

  private final ThrowableHandlingConverter throwableConverter;

  public JsonbEncoder() {
    this.json = JsonStream.builder().build();
    this.properties =
        json.properties("@timestamp", "level", "logger", "message", "thread", "stack_trace");

    final var converter = new ShortenedThrowableConverter();
    converter.setMaxDepthPerThrowable(3);

    final var de = new TrimPackageAbbreviator();
    de.setTargetLength(20);
    converter.setClassNameAbbreviator(de);
    converter.setRootCauseFirst(true);
    throwableConverter = converter; // new ExtendedThrowableProxyConverter(); // converter
  }

  @Override
  public void start() {
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
        100 + extra + message.length() + threadName.length() + loggerName.length();
    final var outputStream = new ByteArrayOutputStream(bufferSize);

    final Instant instant = Instant.ofEpochMilli(event.getTimeStamp());
    try (JsonWriter writer = json.writer(outputStream)) {
      writer.beginObject(properties);
      writer.name(0);
      writer.rawValue(instant.toString());
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
}
