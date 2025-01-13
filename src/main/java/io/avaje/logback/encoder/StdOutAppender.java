package io.avaje.logback.encoder;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.encoder.Encoder;

import java.io.IOException;

/**
 * Appender that writes to STDOUT that defaults to using JsonEncoder.
 */
public final class StdOutAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

  private Encoder<ILoggingEvent> encoder;

  public StdOutAppender() {
    this.encoder = new JsonEncoder();
  }

  @Override
  protected void append(ILoggingEvent event) {
    try {
      System.out.write(encoder.encode(event));
    } catch (IOException e) {
      // NOTE: When actually running on AWS Lambda, an IOException would never happen
      e.printStackTrace();
    }
  }

  @Override
  public void start() {
    encoder.start();
    super.start();
  }

  /**
   * Change the encoder from the default JsonEncoder.
   */
  public void setEncoder(Encoder<ILoggingEvent> encoder) {
    this.encoder = encoder;
  }

  /**
   * Set the component on an underlying JsonEncoder otherwise throw IllegalStateException.
   */
  public void setComponent(String component) {
    if (encoder instanceof JsonEncoder) {
      ((JsonEncoder) encoder).setComponent(component);
    } else {
      throw new IllegalStateException("Can only set component when using JsonEncoder");
    }
  }

  /**
   * Set the environment on an underlying JsonEncoder otherwise throw IllegalStateException.
   */
  public void setEnvironment(String environment) {
    if (encoder instanceof JsonEncoder) {
      ((JsonEncoder) encoder).setEnvironment(environment);
    } else {
      throw new IllegalStateException("Can only set environment when using JsonEncoder");
    }
  }

}
