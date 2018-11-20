package no.nav.helse.streams

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import org.apache.avro.generic.GenericRecord
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Produced

private val serdeConfig = mapOf(
        AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to
                (System.getenv("KAFKA_SCHEMA_REGISTRY_URL") ?: "http://localhost:8081")
)

object Topics {
    val VEDTAK_INFOTRYGD = Topic(
            name = "privat-helse-infotrygd-vedtak",
            keySerde = Serdes.String(),
            valueSerde = Serdes.serdeFrom(JsonSerializer(), JsonDeserializer())

    )

    val VEDTAK_SYKEPENGER = Topic(
            name = "aapen-helse-sykepenger-vedtak",
            keySerde = Serdes.String(),
            valueSerde = Serdes.serdeFrom(JsonSerializer(), JsonDeserializer())
    )
    val VEDTAK_KOMBINERT = Topic(
            name = "privat-helse-vedtak-kombinert",
            keySerde = Serdes.String(),
            valueSerde = Serdes.serdeFrom(JsonSerializer(), JsonDeserializer())
    )

    val VEDTAK_RESULTAT = Topic(
            name = "privat-helse-vedtak-resultat",
            keySerde = Serdes.String(),
            valueSerde = Serdes.String()
    )
}

private fun serdeConfig(env: Environment) = mapOf(
        AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to
                env.schemaRegistryUrl

)

fun configureGenericAvroSerde(serdeConfig: Map<String, Any>): GenericAvroSerde {
    return GenericAvroSerde().apply { configure(serdeConfig, false) }
}

fun <T : SpecificRecord?> configureAvroSerde(serdeConfig: Map<String, Any>): SpecificAvroSerde<T> {
    return SpecificAvroSerde<T>().apply { configure(serdeConfig, false) }
}

fun <K : Any, V : GenericRecord> StreamsBuilder.consumeGenericTopicAvro(topic: Topic<K, V>): KStream<K, V> {
    return stream<K, V>(
            topic.name, Consumed.with(topic.keySerde, topic.valueSerde)
    )
}

fun <K : Any, V : SpecificRecord> StreamsBuilder.consumeTopicAvro(topic: Topic<K, V>): KStream<K, V> {
    return stream<K, V>(
            topic.name, Consumed.with(topic.keySerde, topic.valueSerde)
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
