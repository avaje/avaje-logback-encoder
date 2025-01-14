package io.avaje.logback.encoder;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EvalTest {

  @BeforeAll
  static void beforeAll() {
    System.setProperty("my.property", "x42");
  }

  @Test
  void eval_basic() {
    assertThat(Eval.eval("${my.property}")).isEqualTo("x42");
  }

  @Test
  void eval_defaultValue() {
    assertThat(Eval.eval("${my.property:someDefaultValue}")).isEqualTo("x42");
    assertThat(Eval.eval("${my.other:someDefaultValue}")).isEqualTo("someDefaultValue");
    assertThat(Eval.eval("${my.other}")).isEqualTo("${my.other}");
  }

  @Test
  void eval_nonMatchingEnds() {
    assertThat(Eval.eval("my.property")).isEqualTo("my.property");
    assertThat(Eval.eval("${my.property")).isEqualTo("${my.property");
    assertThat(Eval.eval("my.property}")).isEqualTo("my.property}");
  }

  @Test
  void toSystemPropertyKey() {
    assertThat(Eval.toSystemPropertyKey("FOO")).isEqualTo("foo");
    assertThat(Eval.toSystemPropertyKey("MY_FOO")).isEqualTo("my.foo");
    assertThat(Eval.toSystemPropertyKey("A_MY_FOO")).isEqualTo("a.my.foo");
    assertThat(Eval.toSystemPropertyKey("my.foo")).isEqualTo("my.foo");
  }

  @Test
  void k8sComponent_expected() {
    assertThat(Eval.k8sComponent("some-x-y")).isEqualTo("some");
    assertThat(Eval.k8sComponent("my-some-x-y")).isEqualTo("my-some");
    assertThat(Eval.k8sComponent("foo-bar-some-x-y")).isEqualTo("foo-bar-some");
  }

  @Test
  void k8sComponent_unexpected() {
    assertThat(Eval.k8sComponent(null)).isNull();
    assertThat(Eval.k8sComponent("")).isNull();
    assertThat(Eval.k8sComponent("some")).isNull();
    assertThat(Eval.k8sComponent("some-x")).isNull();
  }
}
