[![Build](https://github.com/avaje/avaje-logback-encoder/actions/workflows/build.yml/badge.svg)](https://github.com/avaje/avaje-logback-encoder/actions/workflows/build.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/avaje/avaje-logback-encoder/blob/master/LICENSE)
[![Maven Central : avaje-record-builder](https://maven-badges.herokuapp.com/maven-central/io.avaje/avaje-logback-encoder/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.avaje/avaje-logback-encoder)
[![Discord](https://img.shields.io/discord/1074074312421683250?color=%237289da&label=discord)](https://discord.gg/Qcqf9R27BR)

# avaje-logback-encoder
Logback encoder that log events as json (similar to Logstash).

Example:
```json
{"timestamp":"2025-01-10T14:47:42.313+13:00","level":"INFO","logger":"org.example.Foo","message":"Hi","thread":"main"}
```

Example with component and environment:
```json
{"component":"my-component","env":"DEV","timestamp":"2025-01-10T14:47:42.313+13:00","level":"INFO","logger":"org.example.Foo","message":"Hi","thread":"main"}
```

## Fields
#### Standard Fields
- `timestamp`
- `level`
- `logger`
- `message`
- `thread`
- `stacktrace`

#### MDC Fields
MDC key/values are included in logged events.

#### Custom Fields
Extra Custom fields can be declared in JSON form, these are added to all logged events.

#### Extra recommended Fields
- `component` - Use to define the "component" (approximately application or a specific component of an application)
- `env` - Use to define the "environment" such as dev, test, prod etc

These default by reading System Environment variables `COMPONENT` and `ENVIRONMENT` respectively and
can also be explicitly set via configuration.



# How to use

#### 1 - Add dependency

For Java 11+ and Logback 1.2.x+ use version `1.0` of the dependency:
```xml
<dependency>
  <groupId>io.avaje</groupId>
  <artifactId>avaje-logback-encoder</artifactId>
  <version>1.0</version>
</dependency>
```

For Java 8 and Logback 1.1.x use version `1.0-java8` of the dependency.
```xml
<dependency>
  <groupId>io.avaje</groupId>
  <artifactId>avaje-logback-encoder</artifactId>
  <version>1.0-java8</version>
</dependency>
```

#### 2 - Use the Encoder in logback.xml

In `logback.xml` specify JsonEncoder as the encoder like:
```xml
<appender name="app" class="your.appender.class">
  <encoder class="io.avaje.logback.encoder.JsonEncoder"/>
</appender>
```

Optionally, configure with `component` and `environment` like:
```xml
<appender name="app" class="your.appender.class">
  <encoder class="io.avaje.logback.encoder.JsonEncoder">
    <component>my-component</component> <!-- OPTIONAL -->
    <enviroment>dev</enviroment>        <!-- OPTIONAL -->
  </encoder>
</appender>
```

Optionally specify custom fields that will appear in every LoggingEvent like:
```xml
<encoder class="io.avaje.logback.encoder.JsonEncoder">
  <customFields>{"appname":"myWebservice","roles":["orders","auth"]}</customFields>
</encoder>
```

#### AWS Lambda / StdOutAppender

For AWS Lambda log events are written to `System.out` and so this also provides
an appender that defaults to using JsonEncoder to write log events to `System.out`.
We can specify to use that appender like:

```xml
<!-- Defaults to using the JsonEncoder -->
<appender class="io.avaje.logback.encoder.StdOutAppender"/>
```

Or with configuration options for component and environment like:

```xml
<appender class="io.avaje.logback.encoder.StdOutAppender">
  <component>my-foo</component>
</appender>
```

Or with an encoder (potentially a different encoder):

```xml
<appender class="io.avaje.logback.encoder.StdOutAppender">
  <encoder class="io.avaje.logback.encoder.JsonEncoder">
    <component>my-foo</component>
    <environment>prod</environment>
    <customFields>{"appname":"myWebservice","buildinfo":42, "roles":["customerorder","auth"],"f":true}</customFields>
  </encoder>
</appender>
```


## Java modules
To ensure `jlink` correctly determines the runtime modules required, add the following to your `module-info.java`:

```java
module my.module {
  requires io.avaje.logback.encoder;
}
```



## Customizing Timestamp

By default, timestamps are written as string values in the format specified by
[`DateTimeFormatter.ISO_OFFSET_DATE_TIME`](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/time/format/DateTimeFormatter.html#ISO_OFFSET_DATE_TIME)
(e.g. `2019-11-03T10:15:30.123+01:00`), in the default TimeZone of the host Java platform.

You can change the pattern like this:

```xml
<encoder class="io.avaje.logback.encoder.JsonEncoder">
    <timestampPattern>yyyy-MM-dd'T'HH:mm:ss.SSS</timestampPattern>
</encoder>
```

The value of the `timestampPattern` can be any of the following:

* `constant` - (e.g. `ISO_OFFSET_DATE_TIME`) timestamp written using the given `DateTimeFormatter` constant
* any other value - (e.g. `yyyy-MM-dd'T'HH:mm:ss.SSS`) timestamp written using a `DateTimeFormatter` created from the given pattern

The formatter uses the default TimeZone of the host Java platform by default. You can change it like this:

```xml
<encoder class="io.avaje.logback.encoder.JsonEncoder">
    <timeZone>UTC</timeZone>
</encoder>
```

The value of the `timeZone` element can be any string accepted by java's `TimeZone.getTimeZone(String id)` method.
For example `America/Los_Angeles`, `GMT+10` or `UTC`.
Use the special value `[DEFAULT]` to use the default TimeZone of the system.
