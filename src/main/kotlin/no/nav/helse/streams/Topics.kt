package no.nav.helse.streams

import org.apache.kafka.common.serialization.*
import org.apache.kafka.streams.*
import org.apache.kafka.streams.kstream.*

object Topics {
   val SYKEPENGESØKNADER_INN = Topic(
      name = "privat-syfo-soknadSendt-v1",
      keySerde = Serdes.String(),
      valueSerde = Serdes.serdeFrom(JsonSerializer(), JsonDeserializer())
   )

   val SYKEPENGESØKNADER_UT = Topic(
      name = "privat-sykepengebehandling",
      keySerde = Serdes.String(),
      valueSerde = Serdes.serdeFrom(JsonSerializer(), JsonDeserializer())
   )
}

fun <K: Any, V: Any> StreamsBuilder.consumeTopic(topic: Topic<K, V>): KStream<K, V> {
   return stream<K, V>(
      topic.name, Consumed.with(topic.keySerde, topic.valueSerde)
   )
}

fun <K, V> KStream<K, V>.toTopic(topic: Topic<K, V>) {
   return to(topic.name, Produced.with(topic.keySerde, topic.valueSerde))
}
