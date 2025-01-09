package io.avaje.logback.encoder;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.encoder.Encoder;

import java.io.IOException;

/**
 * Appender to be used for AWS Lambda that defaults to using JsonEncoder.
 */
public final class AwsLambdaAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

  private Encoder<ILoggingEvent> encoder;

  public AwsLambdaAppender() {
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
}
