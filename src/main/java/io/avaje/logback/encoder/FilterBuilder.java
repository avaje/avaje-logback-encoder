package io.avaje.logback.encoder;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

final class FilterBuilder implements StackElementFilter.Builder {

  private final List<StackElementFilter> filters = new ArrayList<>();

  @Override
  public StackElementFilter.Builder generated() {
    filters.add(new Generated());
    return this;
  }

  @Override
  public StackElementFilter.Builder reflectiveInvocation() {
    filters.add(new ReflectiveInvocation());
    return this;
  }

  @Override
  public StackElementFilter.Builder jdkInternals() {
    filters.add(new JDKInternals());
    return this;
  }

  @Override
  public StackElementFilter.Builder spring() {
    filters.add(new SpringFilter());
    return this;
  }

  @Override
  public StackElementFilter.Builder byPattern(List<Pattern> excludes) {
    if (excludes != null && !excludes.isEmpty()) {
      filters.add(new PatternFilter(excludes));
    }
    return this;
  }

  @Override
  public StackElementFilter.Builder allFilters() {
    generated();
    reflectiveInvocation();
    jdkInternals();
    spring();
    return this;
  }

  @Override
  public StackElementFilter build() {
    if (filters.isEmpty()) {
      return StackElementFilter.any();
    }
    return new Group(filters.toArray(new StackElementFilter[0]));
  }

  private static final class Generated implements StackElementFilter {

    @Override
    public boolean accept(StackTraceElement element) {
      String className = element.getClassName();
      return !className.contains("$$FastClassByCGLIB$$")
        && !className.contains("$$EnhancerBySpringCGLIB$$");
    }
  }

  private static final class ReflectiveInvocation implements StackElementFilter {

    @Override
    public boolean accept(StackTraceElement element) {
      String methodName = element.getMethodName();
      if (methodName.equals("invoke")) {
        String className = element.getClassName();
        return !className.startsWith("sun.reflect.")
          && !className.startsWith("java.lang.reflect.")
          && !className.startsWith("net.sf.cglib.proxy.MethodProxy");
      }
      return true;
    }
  }

  private static final class JDKInternals implements StackElementFilter {

    @Override
    public boolean accept(StackTraceElement element) {
      String className = element.getClassName();
      return !className.startsWith("com.sun.")
        && !className.startsWith("sun.net.");
    }
  }

  private static final class SpringFilter implements StackElementFilter {

    private static final String[] MATCHES = {
      "org.springframework.cglib.",
      "org.springframework.transaction.",
      "org.springframework.validation.",
      "org.springframework.app.",
      "org.springframework.aop.",
      "org.springframework.ws.",
      "org.springframework.web.",
      "org.springframework.transaction"
    };

    @Override
    public boolean accept(StackTraceElement element) {
      String className = element.getClassName();
      if (className.startsWith("org.springframework")) {
        for (String match : MATCHES) {
          if (className.startsWith(match)) {
            return false;
          }
        }
        return true;
      }
      if (className.startsWith("org.apache")) {
        return !className.startsWith("org.apache.tomcat.")
          && !className.startsWith("org.apache.catalina.")
          && !className.startsWith("org.apache.coyote.");
      }
      return true;
    }
  }

  private static final class PatternFilter implements StackElementFilter {

    private final Pattern[] excludes;

    PatternFilter(final List<Pattern> excludes) {
      this.excludes = excludes.toArray(new Pattern[0]);
    }

    @Override
    public boolean accept(StackTraceElement element) {
      final String classNameAndMethod = element.getClassName() + "." + element.getMethodName();
      for (final Pattern exclusionPattern : excludes) {
        if (exclusionPattern.matcher(classNameAndMethod).find()) {
          return false;
        }
      }
      return true;
    }
  }

  private static final class Group implements StackElementFilter {

    private final StackElementFilter[] filters;

    public Group(StackElementFilter[] filters) {
      this.filters = filters;
    }

    @Override
    public boolean accept(StackTraceElement element) {
      for (StackElementFilter filter : filters) {
        if (!filter.accept(element)) {
          return false;
        }
      }
      return true;
    }
  }

}
