package no.nav.helse

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import no.nav.helse.streams.Environment
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.config.SaslConfigs
import java.util.*


 fun resultatConsumer(env: Environment): KafkaConsumer<String, String> {
    return KafkaConsumer(Properties().apply {
        put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
        put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
        put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, ValidatorComponentTest.embeddedEnvironment.schemaRegistry!!.url)
        put(ConsumerConfig.GROUP_ID_CONFIG, "vedtak.result.test")
        put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, ValidatorComponentTest.embeddedEnvironment.brokersURL)
        put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
        put(SaslConfigs.SASL_MECHANISM, "PLAIN")
        put(
                SaslConfigs.SASL_JAAS_CONFIG,
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${env.username}\" password=\"${env.password}\";"
        )
    })
}