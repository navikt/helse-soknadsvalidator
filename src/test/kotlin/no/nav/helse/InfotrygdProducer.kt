package no.nav.helse

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import no.nav.helse.avro.InfoTrygdVedtak
import no.nav.helse.streams.Environment
import no.nav.helse.streams.configureAvroSerde
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import java.util.*

fun infotrygdProducer(env: Environment): KafkaProducer<String, InfoTrygdVedtak> {
    return KafkaProducer(Properties().apply {

        put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, configureAvroSerde<InfoTrygdVedtak>(mapOf(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to env.schemaRegistryUrl)).serializer().javaClass.name)
        put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
        put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, ValidatorComponentTest.embeddedEnvironment.schemaRegistry!!.url)
        put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, ValidatorComponentTest.embeddedEnvironment.brokersURL)
        put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
        put(SaslConfigs.SASL_MECHANISM, "PLAIN")
        put(
                SaslConfigs.SASL_JAAS_CONFIG,
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${env.username}\" password=\"${env.password}\";"
        )
    })
}