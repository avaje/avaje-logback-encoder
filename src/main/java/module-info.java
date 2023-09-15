
module io.avaje.logback {

  exports io.avaje.logback;
  exports io.avaje.logback.abbreviator;

  requires transitive ch.qos.logback.classic;
  requires transitive ch.qos.logback.core;
  requires transitive io.avaje.jsonb;

}
