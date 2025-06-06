/*
 * Copyright 2013-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.avaje.logback.encoder;

import ch.qos.logback.classic.pattern.Abbreviator;
import ch.qos.logback.classic.spi.ClassPackagingData;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.boolex.EvaluationException;
import ch.qos.logback.core.boolex.EventEvaluator;
import ch.qos.logback.core.pattern.Converter;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShortenedThrowableConverterTest {
  /* DEBUG:
   *    Set to true to dump original exception and formatted result on stdout...
   */
  private final boolean dumpOnStdOut = true;

  private static class StackTraceElementGenerator {
    static void generateSingle() {
      oneSingle();
    }

    static void oneSingle() {
      twoSingle();
    }

    private static void twoSingle() {
      threeSingle();
    }

    private static void threeSingle() {
      four();
    }

    private static void four() {
      five();
    }

    private static void five() {
      six();
    }

    private static void six() {
      seven();
    }

    private static void seven() {
      eight();
    }

    private static void eight() {
      throw new RuntimeException("message");
    }

    static void generateCausedBy() {
      oneCausedBy();
    }

    private static void oneCausedBy() {
      twoCausedBy();
    }

    private static void twoCausedBy() {
      try {
        threeSingle();
      } catch (RuntimeException e) {
        throw new RuntimeException("wrapper", e);
      }
    }

    static void generateSuppressed() {
      oneSuppressed();
    }

    private static void oneSuppressed() {
      twoSuppressed();
    }

    private static void twoSuppressed() {
      try {
        threeSingle();
      } catch (RuntimeException e) {
        RuntimeException newException = new RuntimeException();
        newException.addSuppressed(e);
        throw newException;
      }
    }
  }

  @Test
  void testMaxLength() {

    try {
      StackTraceElementGenerator.generateSingle();
      fail("Exception must have been thrown");
    } catch (RuntimeException e) {

      /*
       * First get the un-truncated length
       */
      ShortenedThrowableConverter converter = new ShortenedThrowableConverter();
      converter.setMaxDepthPerThrowable(ShortenedThrowableConverter.FULL_MAX_DEPTH_PER_THROWABLE);
      converter.start();
      String formatted = convert(converter, e);
      int totalLength = formatted.length();

      /*
       * Now truncate and compare
       */
      converter = new ShortenedThrowableConverter();
      converter.setMaxLength(totalLength - 100);
      converter.start();
      formatted = convert(converter, e);

      assertThat(formatted)
          .hasSize(totalLength - 100)
          .endsWith("..." + converter.getLineSeparator());
    }
  }

  @Test
  void testMaxLength_invalidLength() {
    ShortenedThrowableConverter converter = new ShortenedThrowableConverter();

    assertThatThrownBy(() -> converter.setMaxLength(0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> converter.setMaxLength(-10))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void testMaxLength_disable() {
    ShortenedThrowableConverter converter = new ShortenedThrowableConverter();

    converter.setMaxLength(-1);
    converter.start();

    assertThat(converter.getMaxLength()).isEqualTo(ShortenedThrowableConverter.FULL_MAX_LENGTH);
  }

  @Test
  void testMaxLengthPerThrowable() {

    try {
      StackTraceElementGenerator.generateSingle();
      fail("Exception must have been thrown");
    } catch (RuntimeException e) {

      /*
       * First get the un-truncated length
       */
      ShortenedThrowableConverter converter = new ShortenedThrowableConverter();
      converter.setMaxDepthPerThrowable(ShortenedThrowableConverter.FULL_MAX_DEPTH_PER_THROWABLE);
      converter.start();
      String formatted = convert(converter, e);
      int totalLines = countLines(formatted);

      /*
       * Now truncate and compare
       */
      converter = new ShortenedThrowableConverter();
      converter.setMaxDepthPerThrowable(totalLines - 5);
      converter.start();

      formatted = convert(converter, e);

      assertThat(countLines(formatted)).isEqualTo(totalLines - 3);
      assertThat(formatted).contains("4 frames truncated");
    }
  }

  @Test
  void testMaxLengthPerThrowable_invalidLength() {
    ShortenedThrowableConverter converter = new ShortenedThrowableConverter();

    assertThatThrownBy(() -> converter.setMaxDepthPerThrowable(0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> converter.setMaxDepthPerThrowable(-10))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void testMaxLengthPerThrowable_disable() {
    ShortenedThrowableConverter converter = new ShortenedThrowableConverter();

    converter.setMaxDepthPerThrowable(-1);
    converter.start();

    assertThat(converter.getMaxDepthPerThrowable())
        .isEqualTo(ShortenedThrowableConverter.FULL_MAX_DEPTH_PER_THROWABLE);
  }

  @Test
  void testTruncateAfter() {

    try {
      StackTraceElementGenerator.generateSingle();
      fail("Exception must have been thrown");
    } catch (RuntimeException e) {
      ShortenedThrowableConverter converter = new ShortenedThrowableConverter();

      converter.addTruncateAfter("\\.generateSingle$");
      converter.start();
      String formatted = convert(converter, e);

      /* Expected:

         java.lang.RuntimeException: message
             at net.logstash.logback.stacktrace.ShortenedThrowableConverterTest$StackTraceElementGenerator.eight(ShortenedThrowableConverterTest.java:76)
             at net.logstash.logback.stacktrace.ShortenedThrowableConverterTest$StackTraceElementGenerator.seven(ShortenedThrowableConverterTest.java:73)
             at net.logstash.logback.stacktrace.ShortenedThrowableConverterTest$StackTraceElementGenerator.six(ShortenedThrowableConverterTest.java:70)
             at net.logstash.logback.stacktrace.ShortenedThrowableConverterTest$StackTraceElementGenerator.five(ShortenedThrowableConverterTest.java:67)
             at net.logstash.logback.stacktrace.ShortenedThrowableConverterTest$StackTraceElementGenerator.four(ShortenedThrowableConverterTest.java:64)
             at net.logstash.logback.stacktrace.ShortenedThrowableConverterTest$StackTraceElementGenerator.threeSingle(ShortenedThrowableConverterTest.java:61)
             at net.logstash.logback.stacktrace.ShortenedThrowableConverterTest$StackTraceElementGenerator.twoSingle(ShortenedThrowableConverterTest.java:58)
             at net.logstash.logback.stacktrace.ShortenedThrowableConverterTest$StackTraceElementGenerator.oneSingle(ShortenedThrowableConverterTest.java:55)
             at net.logstash.logback.stacktrace.ShortenedThrowableConverterTest$StackTraceElementGenerator.generateSingle(ShortenedThrowableConverterTest.java:52)
             ... 71 frames truncated
      */
      assertThat(formatted)
          .contains("generateSingle")
          .endsWith(
              (e.getStackTrace().length - 9) + " frames truncated" + converter.getLineSeparator());

      assertThat(countLines(formatted)).isEqualTo(11);
    }
  }

  @Test
  void testTruncateAfter_excluded() {

    try {
      StackTraceElementGenerator.generateSingle();
      fail("Exception must have been thrown");
    } catch (RuntimeException e) {
      ShortenedThrowableConverter converter = new ShortenedThrowableConverter();

      converter.addTruncateAfter("\\.generateSingle$");
      converter.addExclude("Single$");
      converter.start();
      String formatted = convert(converter, e);

      /* Expected:

         java.lang.RuntimeException: message
             at net.logstash.logback.stacktrace.ShortenedThrowableConverterTest$StackTraceElementGenerator.eight(ShortenedThrowableConverterTest.java:76)
             at net.logstash.logback.stacktrace.ShortenedThrowableConverterTest$StackTraceElementGenerator.seven(ShortenedThrowableConverterTest.java:73)
             at net.logstash.logback.stacktrace.ShortenedThrowableConverterTest$StackTraceElementGenerator.six(ShortenedThrowableConverterTest.java:70)
             at net.logstash.logback.stacktrace.ShortenedThrowableConverterTest$StackTraceElementGenerator.five(ShortenedThrowableConverterTest.java:67)
             at net.logstash.logback.stacktrace.ShortenedThrowableConverterTest$StackTraceElementGenerator.four(ShortenedThrowableConverterTest.java:64)
             ... 3 frames excluded
             at net.logstash.logback.stacktrace.ShortenedThrowableConverterTest$StackTraceElementGenerator.generateSingle(ShortenedThrowableConverterTest.java:52)
             ... 71 frames truncated
      */
      assertThat(formatted)
          .containsSubsequence("four", "... 3 frames excluded", "generateSingle")
          .endsWith(
              "... "
                  + (e.getStackTrace().length - 9)
                  + " frames truncated"
                  + converter.getLineSeparator());
    }
  }

  /*
   * Use a truncateAfter pattern matching anything
   * -> stop after the first frame.
   */
  @Test
  void testTruncateAfter_matchAll() {

    try {
      StackTraceElementGenerator.generateSingle();
      fail("Exception must have been thrown");
    } catch (RuntimeException e) {
      ShortenedThrowableConverter converter = new ShortenedThrowableConverter();
      converter.addTruncateAfter(".*");
      converter.start();
      String formatted = convert(converter, e);

      /* Expected:

         java.lang.RuntimeException: message
             at net.logstash.logback.stacktrace.ShortenedThrowableConverterTest$StackTraceElementGenerator.eight(ShortenedThrowableConverterTest.java:76)
             ... 79 frames truncated
      */
      assertThat(countLines(formatted)).isEqualTo(3);
      assertThat(formatted)
          .contains("eight")
          .endsWith(
              (e.getStackTrace().length - 1) + " frames truncated" + converter.getLineSeparator());
    }
  }

  /*
   * When truncateAfter kicks-in, the number of common frames omitted is reported on the
   * same line as the total number of truncated lines
   */
  @Test
  void testTruncateAfter_commonFrames() {
    try {
      StackTraceElementGenerator.generateSuppressed();
      fail("Exception must have been thrown");
    } catch (RuntimeException e) {
      ShortenedThrowableConverter converter = new ShortenedThrowableConverter();
      converter.setMaxDepthPerThrowable(8);
      converter.addTruncateAfter("four");
      converter.start();

      String formatted = convert(converter, e);

      /* Expected:

         java.lang.RuntimeException: null
             at net.logstash.logback.stacktrace.ShortenedThrowableConverterTest$StackTraceElementGenerator.twoSuppressed(ShortenedThrowableConverterTest.java:101)
             at net.logstash.logback.stacktrace.ShortenedThrowableConverterTest$StackTraceElementGenerator.oneSuppressed(ShortenedThrowableConverterTest.java:95)
             at net.logstash.logback.stacktrace.ShortenedThrowableConverterTest$StackTraceElementGenerator.generateSuppressed(ShortenedThrowableConverterTest.java:92)
             at net.logstash.logback.stacktrace.ShortenedThrowableConverterTest.testTruncateAfter_commonFrames(ShortenedThrowableConverterTest.java:473)
             at sun.reflect.NativeMethodAccessorImpl.invoke0(NativeMethodAccessorImpl.java)
             at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
             at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
             at java.lang.reflect.Method.invoke(Method.java:498)
             ... 66 frames truncated
             Suppressed: java.lang.RuntimeException: message
                 at net.logstash.logback.stacktrace.ShortenedThrowableConverterTest$StackTraceElementGenerator.eight(ShortenedThrowableConverterTest.java:76)
                 at net.logstash.logback.stacktrace.ShortenedThrowableConverterTest$StackTraceElementGenerator.seven(ShortenedThrowableConverterTest.java:73)
                 at net.logstash.logback.stacktrace.ShortenedThrowableConverterTest$StackTraceElementGenerator.six(ShortenedThrowableConverterTest.java:70)
                 at net.logstash.logback.stacktrace.ShortenedThrowableConverterTest$StackTraceElementGenerator.five(ShortenedThrowableConverterTest.java:67)
                 at net.logstash.logback.stacktrace.ShortenedThrowableConverterTest$StackTraceElementGenerator.four(ShortenedThrowableConverterTest.java:64)
                 ... 75 frames truncated (including 73 common frames)
      */
      assertThat(formatted)
          .containsSubsequence(
              (e.getStackTrace().length - 8) + " frames truncated",
              "Suppressed",
              "... "
                  + (e.getSuppressed()[0].getStackTrace().length - 5)
                  + " frames truncated (including ")
          .endsWith("common frames)" + converter.getLineSeparator());
    }
  }

  @Test
  void testExclusion_consecutive() {

    try {
      StackTraceElementGenerator.generateSingle();
      fail("Exception must have been thrown");
    } catch (RuntimeException e) {
      ShortenedThrowableConverter converter = new ShortenedThrowableConverter();
      converter.addExclude("one");
      converter.addExclude("two");
      converter.addExclude("four");
      converter.addExclude("five");
      converter.addExclude("six");
      converter.setMaxDepthPerThrowable(8);
      converter.start();

      String formatted = convert(converter, e);
      assertThat(formatted).containsSubsequence("3 frames excluded", "2 frames excluded");
      assertThat(countLines(formatted)).isEqualTo(12);
    }
  }

  /*
   * Exclude match only one element -> it should not be excluded
   */
  @Test
  void testExclusion_noConsecutive() {

    try {
      StackTraceElementGenerator.generateSingle();
      fail("Exception must have been thrown");
    } catch (RuntimeException e) {
      ShortenedThrowableConverter converter = new ShortenedThrowableConverter();
      converter.addExclude("one");
      converter.setMaxDepthPerThrowable(8);
      converter.start();

      String formatted = convert(converter, e);
      assertThat(formatted).contains("one").doesNotContain("frames excluded");
      assertThat(countLines(formatted)).isEqualTo(10);
    }
  }

  @Test
  void testExclusion_atEnd() {

    try {
      StackTraceElementGenerator.generateSingle();
      fail("Exception must have been thrown");
    } catch (RuntimeException e) {

      /*
       * First get the un-truncated stacktrace
       */
      ShortenedThrowableConverter converter = new ShortenedThrowableConverter();
      converter.setMaxDepthPerThrowable(ShortenedThrowableConverter.FULL_MAX_DEPTH_PER_THROWABLE);
      converter.start();
      String formatted = convert(converter, e);

      /*
       * Find the last two frames
       */

      List<String> lines = getLines(formatted);

      /*
       * Now truncate and compare
       */
      converter = new ShortenedThrowableConverter();
      converter.addExclude(extractClassAndMethod(lines.get(lines.size() - 2)) + "$");
      converter.addExclude(extractClassAndMethod(lines.get(lines.size() - 1)) + "$");
      converter.start();
      formatted = convert(converter, e);

      assertThat(formatted).endsWith("2 frames excluded" + converter.getLineSeparator());
      assertThat(countLines(formatted)).isEqualTo(lines.size() - 1);
    }
  }

  @Test
  void testAddExclusion() {
    ShortenedThrowableConverter converter = new ShortenedThrowableConverter();
    converter.addExclude(".*");
    converter.addExclude("foo.bar");
    converter.start();

    assertThat(converter.getExcludes()).containsExactly(".*", "foo.bar");
  }

  /*
   * Do not repeat packaging data unless it differs from previous line
   */
  @Test
  void testPackagingData() {

    // Add fake packaging data for the test
    //
    ThrowableProxy proxy = new ThrowableProxy(new Exception("boum"));
    proxy.getStackTraceElementProxyArray()[0].setClassPackagingData(
        new ClassPackagingData("test.jar", "1.2.3"));
    proxy.getStackTraceElementProxyArray()[1].setClassPackagingData(
        new ClassPackagingData("test.jar", "1.2.3"));
    proxy.getStackTraceElementProxyArray()[2].setClassPackagingData(
        new ClassPackagingData("another.jar", "1.2.3"));
    proxy.getStackTraceElementProxyArray()[3].setClassPackagingData(
        new ClassPackagingData("another.jar", "1.2.3"));

    // Render the exception
    //
    ShortenedThrowableConverter converter = new ShortenedThrowableConverter();
    converter.start();
    String formatted = convert(converter, proxy);

    /* Expected:

       java.lang.Exception: Boum
           at net.logstash.logback.stacktrace.ShortenedThrowableConverterTest.testDoNotRepeatPackagingData(ShortenedThrowableConverterTest.java:411) [test.jar:1.2.3]
           at sun.reflect.NativeMethodAccessorImpl.invoke0(NativeMethodAccessorImpl.java)
           at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62) [another.jar:1.2.3]
           at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
           at java.lang.reflect.Method.invoke(Method.java:498)
    */
    List<String> lines = getLines(formatted);
    assertThat(lines.get(1)).endsWith("[test.jar:1.2.3]");
    assertThat(lines.get(2)).endsWith(")");
    assertThat(lines.get(3)).endsWith("[another.jar:1.2.3]");
    assertThat(lines.get(4)).endsWith(")");
  }

  @Test
  void testOmmitCommonFrames() {
    try {
      StackTraceElementGenerator.generateCausedBy();
      fail("Exception must have been thrown");
    } catch (RuntimeException e) {
      // Omit common frames (the default)
      //
      ShortenedThrowableConverter converter = new ShortenedThrowableConverter();
      converter.start();

      String formatted = convert(converter, e);
      assertThat(formatted).containsSubsequence("Caused by", "common frames omitted");

      // Keep common frames
      //
      converter = new ShortenedThrowableConverter();
      converter.setOmitCommonFrames(false);
      converter.start();

      formatted = convert(converter, e);
      assertThat(formatted).contains("Caused by").doesNotContain("common frames omitted");
    }
  }

  @Test
  void testCausedBy() {

    try {
      StackTraceElementGenerator.generateCausedBy();
      fail("Exception must have been thrown");
    } catch (RuntimeException e) {
      ShortenedThrowableConverter converter = new ShortenedThrowableConverter();
      converter.setMaxDepthPerThrowable(8);
      converter.start();

      String formatted = convert(converter, e);
      assertThat(formatted)
          .containsSubsequence("wrapper", "Caused by", "message", "common frames omitted");
    }
  }

  @Test
  void testRootCauseFirst() {

    try {
      StackTraceElementGenerator.generateCausedBy();
      fail("Exception must have been thrown");
    } catch (RuntimeException e) {
      ShortenedThrowableConverter converter = new ShortenedThrowableConverter();
      converter.setRootCauseFirst(true);
      converter.setMaxDepthPerThrowable(8);
      converter.start();

      String formatted = convert(converter, e);
      assertThat(formatted)
          .containsSubsequence("message", "common frames omitted", "Wrapped by", "wrapper");
    }
  }

  @Test
  void testEvaluator() throws EvaluationException {

    try {
      StackTraceElementGenerator.generateCausedBy();
      fail("Exception must have been thrown");
    } catch (RuntimeException e) {
      ShortenedThrowableConverter converter = new ShortenedThrowableConverter();

      @SuppressWarnings("unchecked")
      EventEvaluator<ILoggingEvent> evaluator = mock(EventEvaluator.class);
      when(evaluator.evaluate(any(ILoggingEvent.class))).thenReturn(true);
      converter.addEvaluator(evaluator);
      converter.start();

      String formatted = convert(converter, e);
      assertThat(formatted).isEmpty();
    }
  }

  @SuppressWarnings("rawtypes")
  @Test
  void testOptions() throws EvaluationException {

    EventEvaluator evaluator = mock(EventEvaluator.class);
    Map<String, EventEvaluator> evaluatorMap = new HashMap<>();
    evaluatorMap.put("evaluator", evaluator);

    Context context = mock(Context.class);
    when(context.getObject(CoreConstants.EVALUATOR_MAP)).thenReturn(evaluatorMap);

    ShortenedThrowableConverter converter = new ShortenedThrowableConverter();
    converter.setContext(context);

    // test full values
    converter.setOptionList(
        Arrays.asList(
            "full", "full", "full", "rootFirst", "evaluator", "regex", "omitCommonFrames"));
    converter.start();

    assertThat(converter.getMaxDepthPerThrowable())
        .isEqualTo(ShortenedThrowableConverter.FULL_MAX_DEPTH_PER_THROWABLE);
    assertThat(converter.getShortenedClassNameLength())
        .isEqualTo(ShortenedThrowableConverter.FULL_CLASS_NAME_LENGTH);
    assertThat(converter.getMaxLength()).isEqualTo(ShortenedThrowableConverter.FULL_MAX_LENGTH);
    assertThat(converter.isRootCauseFirst()).isTrue();
    assertThat(converter.getLineSeparator()).isEqualTo(CoreConstants.LINE_SEPARATOR);
    assertThat(converter.getEvaluators().get(0)).isEqualTo(evaluator);
    assertThat(converter.getExcludes().get(0)).isEqualTo("regex");
    assertThat(converter.isOmitCommonFrames()).isTrue();

    // test short values
    converter.setOptionList(
        Arrays.asList(
            "short",
            "short",
            "short",
            "rootFirst",
            "inlineHash",
            "inline",
            "evaluator",
            "regex",
            "keepCommonFrames"));
    converter.start();

    assertThat(converter.getMaxDepthPerThrowable())
        .isEqualTo(ShortenedThrowableConverter.SHORT_MAX_DEPTH_PER_THROWABLE);
    assertThat(converter.getShortenedClassNameLength())
        .isEqualTo(ShortenedThrowableConverter.SHORT_CLASS_NAME_LENGTH);
    assertThat(converter.getMaxLength()).isEqualTo(ShortenedThrowableConverter.SHORT_MAX_LENGTH);
    assertThat(converter.getLineSeparator())
        .isEqualTo(ShortenedThrowableConverter.DEFAULT_INLINE_SEPARATOR);
    assertThat(converter.isOmitCommonFrames()).isFalse();

    // test numeric values
    converter.setOptionList(Arrays.asList("1", "2", "3"));
    converter.start();
    assertThat(converter.getMaxDepthPerThrowable()).isEqualTo(1);
    assertThat(converter.getShortenedClassNameLength()).isEqualTo(2);
    assertThat(converter.getMaxLength()).isEqualTo(3);

    // test invalid numeric values
    converter.setOptionList(Arrays.asList("a", "b", "c"));
    converter.start();
    assertThat(converter.getMaxDepthPerThrowable())
        .isEqualByComparingTo(ShortenedThrowableConverter.DEFAULT_MAX_DEPTH_PER_THROWABLE);
    assertThat(converter.getShortenedClassNameLength())
        .isEqualByComparingTo(ShortenedThrowableConverter.DEFAULT_CLASS_NAME_LENGTH);
    assertThat(converter.getMaxLength())
        .isEqualByComparingTo(ShortenedThrowableConverter.DEFAULT_MAX_LENGTH);
  }

  @Test
  void testSuppressed() {

    try {
      StackTraceElementGenerator.generateSuppressed();
      fail("Exception must have been thrown");
    } catch (RuntimeException e) {
      ShortenedThrowableConverter converter = new ShortenedThrowableConverter();
      converter.setMaxDepthPerThrowable(8);
      converter.start();

      String formatted = convert(converter, e);
      assertThat(formatted).contains("Suppressed").contains("common frames omitted");
    }
  }

  @Test
  void testShortenedClassName() {

    try {
      StackTraceElementGenerator.generateSingle();
      fail("Exception must have been thrown");
    } catch (RuntimeException e) {
      ShortenedThrowableConverter converter = new ShortenedThrowableConverter();
      converter.setShortenedClassNameLength(10);
      converter.start();

      String formatted = convert(converter, e);
      assertThat(formatted).doesNotContain(getClass().getPackage().getName()).contains("i.a.l.");
    }
  }

  @Test
  void testShortenedClassName_disable() {
    ShortenedThrowableConverter converter = new ShortenedThrowableConverter();

    converter.setShortenedClassNameLength(-1);
    converter.start();

    assertThat(converter.getShortenedClassNameLength())
        .isEqualTo(ShortenedThrowableConverter.FULL_CLASS_NAME_LENGTH);
  }

  @Test
  void testShortenedClassName_customAbbreviator() {
    Abbreviator abbreviator = in -> "foo";

    ShortenedThrowableConverter converter = new ShortenedThrowableConverter();
    converter.setClassNameAbbreviator(abbreviator);
    converter.start();

    String formatted = convert(converter, new Exception());

    assertThat(converter.getClassNameAbbreviator()).isEqualTo(abbreviator);
    assertThat(getLines(formatted))
        .allMatch(l -> l.startsWith("foo:") || l.trim().startsWith("at foo."));
  }

  // --------------------------------------------------------------------------------------------

  private String convert(Converter<ILoggingEvent> converter, ThrowableProxy proxy) {
    return convert(converter, createEvent(proxy));
  }

  private String convert(Converter<ILoggingEvent> converter, Throwable e) {
    return convert(converter, createEvent(e));
  }

  private String convert(Converter<ILoggingEvent> converter, ILoggingEvent event) {
    String formatted = converter.convert(event);
    if (dumpOnStdOut) {
      Throwable t = ((ThrowableProxy) event.getThrowableProxy()).getThrowable();
      t.printStackTrace();
      System.out.println(formatted);
    }
    return formatted;
  }

  private String extractClassAndMethod(String string) {
    int atIndex = string.indexOf("at ");
    int endIndex = string.indexOf('(');
    return string.substring(atIndex + 3, endIndex);
  }

  private List<String> getLines(String formatted) {
    List<String> lines = new ArrayList<>();
    try {
      BufferedReader reader = new BufferedReader(new StringReader(formatted));
      String line = null;
      while ((line = reader.readLine()) != null) {
        lines.add(line);
      }
      return lines;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private int countLines(String formatted) {
    return getLines(formatted).size();
  }

  private ILoggingEvent createEvent(Throwable e) {
    return createEvent(new ThrowableProxy(e));
  }

  private ILoggingEvent createEvent(ThrowableProxy proxy) {
    ILoggingEvent event = mock(ILoggingEvent.class);
    when(event.getThrowableProxy()).thenReturn(proxy);
    return event;
  }

  private List<String> extractStackHashes(String formattedStackTrace) {
    Pattern hashPattern = Pattern.compile("<#([0-9abcdef]{8})>");
    Matcher matcher = hashPattern.matcher(formattedStackTrace);
    List<String> hashes = new ArrayList<>();
    while (matcher.find()) {
      hashes.add(matcher.group(1));
    }
    return hashes;
  }
}
