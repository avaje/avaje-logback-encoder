package io.avaje.logback.encoder;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class StackElementFilterTest {

  @Test
  void generated() {
    StackElementFilter filter = StackElementFilter.builder().generated().build();

    assertTrue(filter.accept(new StackTraceElement("a.b.C", "foo", null, 0)));

    assertFalse(filter.accept(new StackTraceElement("a.b.C$$FastClassByCGLIB$$", "foo", null, 0)));
    assertFalse(filter.accept(new StackTraceElement("a.b.C$$FastClassByCGLIB$$D", "foo", null, 0)));
    assertFalse(filter.accept(new StackTraceElement("a.b.$$EnhancerBySpringCGLIB$$", "foo", null, 0)));
    assertFalse(filter.accept(new StackTraceElement("a.b.$$EnhancerBySpringCGLIB$$D", "foo", null, 0)));
  }

  @Test
  void reflectiveInvoke() {
    StackElementFilter filter = StackElementFilter.builder().reflectiveInvocation().build();

    assertTrue(filter.accept(new StackTraceElement("a.b.C", "invoke", null, 0)));
    assertFalse(filter.accept(new StackTraceElement("java.lang.reflect.A", "notInvoke", null, 0)));
    assertFalse(filter.accept(new StackTraceElement("sun.reflect.A", "notInvoke", null, 0)));
    assertFalse(filter.accept(new StackTraceElement("net.sf.cglib.proxy.MethodProxy", "notInvoke", null, 0)));

    assertFalse(filter.accept(new StackTraceElement("java.lang.reflect.A", "invoke", null, 0)));
    assertFalse(filter.accept(new StackTraceElement("sun.reflect.A", "invoke", null, 0)));
    assertFalse(filter.accept(new StackTraceElement("net.sf.cglib.proxy.MethodProxy", "invoke", null, 0)));
  }

  @Test
  void jdkInternals() {
    StackElementFilter filter = StackElementFilter.builder().jdkInternals().build();

    assertTrue(filter.accept(new StackTraceElement("a.b.C", "invoke", null, 0)));
    assertTrue(filter.accept(new StackTraceElement("java.lang.C", "invoke", null, 0)));

    assertFalse(filter.accept(new StackTraceElement("com.sun.A", "any", null, 0)));
    assertFalse(filter.accept(new StackTraceElement("sun.net.A", "any", null, 0)));
  }

  @Test
  void spring() {
    StackElementFilter filter = StackElementFilter.builder().spring().build();

    // accepted
    assertTrue(filter.accept(new StackTraceElement("a.b.C", "any", null, 0)));
    assertTrue(filter.accept(new StackTraceElement("org.springframework.cglibX", "any", null, 0)));
    assertTrue(filter.accept(new StackTraceElement("org.springframework.foo", "any", null, 0)));
    assertTrue(filter.accept(new StackTraceElement("org.springframework.Foo", "any", null, 0)));

    // filtered out
    final String[] prefixes = new String[]{
      "org.springframework.cglib.",
      "org.springframework.transaction.",
      "org.springframework.validation.",
      "org.springframework.app.",
      "org.springframework.aop.",
      "org.springframework.ws.",
      "org.springframework.web.",
      "org.springframework.transaction"
    };

    for (String prefix : prefixes) {
      assertThat(filter.accept(new StackTraceElement(prefix, "any", null, 0)))
        .describedAs("prefix of "+prefix)
        .isFalse();
    }
  }

  @Test
  void patterns() {
    List<Pattern> patterns = new ArrayList<>();
    patterns.add(Pattern.compile("^java\\.util\\.concurrent\\.ThreadPoolExecutor\\.runWorker"));
    patterns.add(Pattern.compile("^java\\.lang\\.Thread\\.run$"));
    patterns.add(Pattern.compile("My\\$Foo"));

    StackElementFilter filter = StackElementFilter.builder().byPattern(patterns).build();

    assertFalse(filter.accept(new StackTraceElement("java.util.concurrent.ThreadPoolExecutor", "runWorker", null, 0)));
    assertFalse(filter.accept(new StackTraceElement("java.util.concurrent.ThreadPoolExecutor", "runWorkerA", null, 0)));

    assertFalse(filter.accept(new StackTraceElement("java.lang.Thread", "run", null, 0)));
    assertTrue(filter.accept(new StackTraceElement("java.lang.Thread", "runX", null, 0)));
    assertTrue(filter.accept(new StackTraceElement("java.lang.Thread", "xrun", null, 0)));

    assertFalse(filter.accept(new StackTraceElement("org.My$Foo", "any", null, 0)));
    assertFalse(filter.accept(new StackTraceElement("org.BeforeMy$Foo", "any", null, 0)));
    assertFalse(filter.accept(new StackTraceElement("org.BeforeMy$FooAfter", "any", null, 0)));
  }

}
