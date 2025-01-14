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

import java.util.List;
import java.util.regex.Pattern;

/**
 * Component in charge of accepting or rejecting {@link StackTraceElement elements} when computing a
 * stack trace hash
 */
public interface StackElementFilter {

  /**
   * Return a Builder for common stack element filters.
   */
  static Builder builder() {
    return new FilterBuilder();
  }

  /**
   * Tests whether the specified {@link StackTraceElement} should be accepted when computing
   * a stack hash.
   *
   * @param element The {@link StackTraceElement} to be tested
   * @return {@code true} if and only if {@code element} should be accepted
   */
  boolean accept(StackTraceElement element);

  /**
   * Creates a {@link StackElementFilter} that accepts any stack trace elements
   *
   * @return the filter
   */
  static StackElementFilter any() {
    return element -> true;
  }

  /**
   * Creates a {@link StackElementFilter} that accepts all stack trace elements with a non {@code
   * null} {@code {@link StackTraceElement#getFileName()} filename} and positive {@link
   * StackTraceElement#getLineNumber()} line number}
   *
   * @return the filter
   */
  static StackElementFilter withSourceInfo() {
    return element -> element.getFileName() != null && element.getLineNumber() >= 0;
  }

  /**
   * Creates a {@link StackElementFilter} by exclusion {@link Pattern patterns}
   *
   * @param excludes regular expressions matching {@link StackTraceElement} to filter out
   * @return the filter
   */
  static StackElementFilter byPattern(final List<Pattern> excludes) {
    return builder().byPattern(excludes).build();
  }

  /**
   * Builder for common StackElementFilters.
   */
  interface Builder {

    /**
     * Include generated classes in the filter containing
     * {@code $$FastClassByCGLIB$$} and {@code $$EnhancerBySpringCGLIB$$}
     */
    Builder generated();

    /**
     * Include reflective invocation in the filter.
     */
    Builder reflectiveInvocation();

    /**
     * Include jdk internal classes in the filter.
     */
    Builder jdkInternals();

    /**
     * Include Spring Framework dynamic invocation and plumbing in the filter.
     */
    Builder spring();

    /**
     * Include the regex patterns in the filter.
     */
    Builder byPattern(List<Pattern> excludes);

    /**
     * Include all the standard filters generated, reflective invocation, jdk internals and spring.
     */
    Builder allFilters();

    /**
     * Build and return the StackElementFilter with the given options.
     */
    StackElementFilter build();

  }
}
