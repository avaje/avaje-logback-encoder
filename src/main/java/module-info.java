
module io.avaje.logback.encoder {

  exports io.avaje.logback.encoder;
  exports io.avaje.logback.encoder.abbreviator;

  requires transitive ch.qos.logback.classic;
  requires transitive ch.qos.logback.core;
  requires transitive io.avaje.jsonb;

}
