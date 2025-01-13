package io.avaje.logback.encoder;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.encoder.Encoder;

import java.io.IOException;

/**
 * Appender that writes to STDOUT that defaults to using JsonEncoder.
 */
public final class StdOutAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

  private JsonEncoder encoder;

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
  public void setEncoder(JsonEncoder encoder) {
    this.encoder = encoder;
  }

  /**
   * Set the component on an underlying JsonEncoder otherwise throw IllegalStateException.
   */
  public void setComponent(String component) {
    encoder.setComponent(component);
  }

  /**
   * Set the environment on an underlying JsonEncoder otherwise throw IllegalStateException.
   */
  public void setEnvironment(String environment) {
    encoder.setEnvironment(environment);
  }

}
