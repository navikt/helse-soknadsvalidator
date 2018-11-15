package no.nav.helse

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import no.nav.helse.streams.Environment
import no.nav.helse.streams.Topics
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.json.JSONObject
import java.util.*

fun infotrygdProducer(env: Environment): KafkaProducer<String, JSONObject> {
    return KafkaProducer(Properties().apply {

        put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, Topics.VEDTAK_INFOTRYGD.valueSerde.serializer().javaClass.name)
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