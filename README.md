[![Build](https://github.com/avaje/avaje-logback-encoder/actions/workflows/build.yml/badge.svg)](https://github.com/avaje/avaje-logback-encoder/actions/workflows/build.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/avaje/avaje-logback-encoder/blob/master/LICENSE)
[![Maven Central : avaje-record-builder](https://maven-badges.herokuapp.com/maven-central/io.avaje/avaje-logback-encoder/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.avaje/avaje-logback-encoder)
[![Discord](https://img.shields.io/discord/1074074312421683250?color=%237289da&label=discord)](https://discord.gg/Qcqf9R27BR)

# avaje-logback-encoder
logback encoder that uses avaje-jsonb to log events as json

## Usage

Add the encoder to your appender
```xml
<appender name="app" class="your.appender.class">
    <encoder class="io.avaje.logback.encoder.JsonbEncoder">
        <-- configuration -->
    </encoder>
</appender>
```

## Global Custom Fields

Add custom fields that will appear in every LoggingEvent like this :

```xml
<encoder class="io.avaje.logback.encoder.JsonbEncoder">
    <customFields>{"appname":"myWebservice","roles":["customerorder","auth"],"buildinfo":{"version":"Version 0.1.0-SNAPSHOT","lastcommit":"75473700d5befa953c45f630c6d9105413c16fe1"}}</customFields>
</encoder>
```

## Customizing Timestamp

By default, timestamps are written as string values in the format specified by
[`DateTimeFormatter.ISO_OFFSET_DATE_TIME`](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/time/format/DateTimeFormatter.html#ISO_OFFSET_DATE_TIME)
(e.g. `2019-11-03T10:15:30.123+01:00`), in the default TimeZone of the host Java platform.

You can change the pattern like this:

```xml
<encoder class="io.avaje.logback.encoder.JsonbEncoder">
    <timestampPattern>yyyy-MM-dd'T'HH:mm:ss.SSS</timestampPattern>
</encoder>
```

The value of the `timestampPattern` can be any of the following:

* `constant` - (e.g. `ISO_OFFSET_DATE_TIME`) timestamp written using the given `DateTimeFormatter` constant
* any other value - (e.g. `yyyy-MM-dd'T'HH:mm:ss.SSS`) timestamp written using a `DateTimeFormatter` created from the given pattern

The formatter uses the default TimeZone of the host Java platform by default. You can change it like this:

```xml
<encoder class="io.avaje.logback.encoder.JsonbEncoder">
    <timeZone>UTC</timeZone>
</encoder>
```

The value of the `timeZone` element can be any string accepted by java's `TimeZone.getTimeZone(String id)` method.
For example `America/Los_Angeles`, `GMT+10` or `UTC`.
Use the special value `[DEFAULT]` to use the default TimeZone of the system.
