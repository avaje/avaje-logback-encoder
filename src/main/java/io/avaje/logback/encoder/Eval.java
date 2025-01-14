package io.avaje.logback.encoder;

/**
 * Helper to evaluate expressions like {@code "${my.property}"}, {@code "${MY_PROPERTY:someDefaultValue}"} etc.
 */
final class Eval {

  /**
   * Return the default component value using environment variables.
   * <p>
   * For K8s this derives the component name from the HOSTNAME.
   */
  static String defaultComponent() {
    String component = System.getenv("COMPONENT");
    if (component != null) {
      return component;
    }
    if (System.getenv("KUBERNETES_PORT") != null) {
      // in k8s we can default off the hostname
      return k8sComponent(System.getenv("HOSTNAME"));
    }
    return null;
  }

  static String k8sComponent(String hostname) {
    if (hostname == null) {
      return null;
    }
    int p0 = hostname.lastIndexOf('-');
    if (p0 > 1) {
      int p1 = hostname.lastIndexOf('-', p0 - 1);
      if (p1 > 0) {
        return hostname.substring(0, p1);
      }
    }
    return null;
  }

  /**
   * Evaluate the expression and otherwise return the original value.
   * <p>
   * Expressions are in the form {@code ${key:defaultValue}}
   * <p>
   * Examples:
   * <pre>{@code
   *
   *  ${APP_ENV:localDev}
   *  ${system.name:unknown}
   *  ${MY_COMPONENT:myDefaultValue}
   *
   * }</pre>
   */
  static String eval(String value) {
    if (value == null || !value.startsWith("${") || !value.endsWith("}")) {
      return value;
    }
    String raw = value.substring(2, value.length() - 1);
    String[] split = raw.split(":", 2);
    String key = split[0];
    String val = System.getProperty(key);
    if (val != null) {
      return val;
    }
    val = System.getenv(key);
    if (val != null) {
      return val;
    }
    val = System.getProperty(toSystemPropertyKey(key));
    if (val != null) {
      return val;
    }
    return split.length == 2 ? split[1] : value;
  }

  static String toSystemPropertyKey(String key) {
    return key.replace('_', '.').toLowerCase();
  }
}
