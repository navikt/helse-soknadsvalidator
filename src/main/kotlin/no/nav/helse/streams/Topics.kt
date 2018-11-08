package no.nav.helse.streams

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.helse.avro.InfoTrygdVedtak
import no.nav.helse.avro.SykePengeVedtak
import no.nav.helse.avro.Vedtak
import org.apache.avro.generic.GenericRecord
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.*
import org.apache.kafka.streams.*
import org.apache.kafka.streams.kstream.*

private val serdeConfig = mapOf(
        AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to
                (System.getenv("KAFKA_SCHEMA_REGISTRY_URL") ?: "http://localhost:8081")
)

object Topics {
   val VEDTAK_INFOTRYGD = Topic(
           name = "vedtak.infotrygd",
           keySerde = Serdes.String(),
           valueSerde = configureAvroSerde<InfoTrygdVedtak>(serdeConfig)
   )

   val VEDTAK_SYKEPENGER = Topic(
           name = "vedtak.sykepenger",
           keySerde = Serdes.String(),
           valueSerde = configureAvroSerde<SykePengeVedtak>(serdeConfig)
   )
   val VEDTAK_KOMBINERT = Topic(
           name = "vedtak.kombinert",
           keySerde = Serdes.String(),
           valueSerde = configureAvroSerde<Vedtak>(serdeConfig)
   )

   val VEDTAK_RESULTAT = Topic(
           name = "vedtak.resultat",
           keySerde = Serdes.String(),
           valueSerde = configureGenericAvroSerde(serdeConfig)
   )
}
fun configureGenericAvroSerde(serdeConfig: Map<String, Any>): GenericAvroSerde {
   return GenericAvroSerde().apply { configure(serdeConfig, false) }
}

fun <T : SpecificRecord?> configureAvroSerde(serdeConfig: Map<String, Any>): SpecificAvroSerde<T> {
   return SpecificAvroSerde<T>().apply { configure(serdeConfig, false) }
}

fun <K : Any, V : GenericRecord> StreamsBuilder.consumeGenericTopic(topic: Topic<K, V>): KStream<K, V> {
   return stream<K, V>(
           topic.name, Consumed.with(topic.keySerde, topic.valueSerde)
   )
}

fun <K : Any, V : SpecificRecord> StreamsBuilder.consumeTopic(topic: Topic<K, V>): KStream<K, V> {
   return stream<K, V>(
           topic.name, Consumed.with(topic.keySerde, topic.valueSerde)
   )
}

fun <K, V> KStream<K, V>.toTopic(topic: Topic<K, V>) {
   return to(topic.name, Produced.with(topic.keySerde, topic.valueSerde))
}
