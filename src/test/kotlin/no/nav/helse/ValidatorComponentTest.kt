package no.nav.helse

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import no.nav.common.JAASCredential
import no.nav.common.KafkaEnvironment
import no.nav.helse.avro.InfoTrygdVedtak
import no.nav.helse.avro.SykePengeVedtak
import no.nav.helse.streams.Environment
import no.nav.helse.streams.Topics
import no.nav.helse.streams.configureAvroSerde
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.SaslConfigs
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import java.time.Duration
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ValidatorComponentTest {

    companion object {
        private const val username = "srvkafkaclient"
        private const val password = "kafkaclient"

        val embeddedEnvironment = KafkaEnvironment(
                users = listOf(JAASCredential(username, password)),
                autoStart = false,
                withSchemaRegistry = true,
                withSecurity = true,
                topics = listOf("vedtak.infotrygd", "vedtak.sykepenger", "vedtak.resultat", "vedtak.kombinert")
        )

        @BeforeClass
        @JvmStatic
        fun setup() {
            embeddedEnvironment.start()
        }

        @AfterClass
        @JvmStatic
        fun teardown() {
            embeddedEnvironment.tearDown()
        }
    }


    @Test
    fun ` embedded kafka cluster is up and running `() {
        assertEquals(embeddedEnvironment.serverPark.status, KafkaEnvironment.ServerParkStatus.Started)

        assertNotEquals(embeddedEnvironment.serverPark.schemaRegStatus, KafkaEnvironment.SchemaRegistryStatus.NotAvailable)
    }

    @Test
    fun ` the very first test`() {
        assertEquals(embeddedEnvironment.serverPark.status, KafkaEnvironment.ServerParkStatus.Started)
    }

    @Test
    fun ` producing a message `() {
        val producer = KafkaProducer<String, String>(Properties().apply {
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
            put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, embeddedEnvironment.schemaRegistry!!.url)
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedEnvironment.brokersURL)
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
            put(SaslConfigs.SASL_MECHANISM, "PLAIN")
            put(
                    SaslConfigs.SASL_JAAS_CONFIG,
                    "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${username}\" password=\"${password}\";"
            )
        })
        producer.send(ProducerRecord<String, String>("vedtak.resultat", "id", "string"))
    }

    @Test
    fun ` two vedtak with equal amounts are counted`() {

        // given an environment
        val env = Environment(
                username = username,
                password = password,
                bootstrapServersUrl = embeddedEnvironment.brokersURL,
                schemaRegistryUrl = embeddedEnvironment.schemaRegistry!!.url
        )

        val infotrygProducer = KafkaProducer<String, InfoTrygdVedtak>(Properties().apply {

            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, configureAvroSerde<InfoTrygdVedtak>(mapOf(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to env.schemaRegistryUrl)).serializer().javaClass.name)
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
            put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, embeddedEnvironment.schemaRegistry!!.url)
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedEnvironment.brokersURL)
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
            put(SaslConfigs.SASL_MECHANISM, "PLAIN")
            put(
                    SaslConfigs.SASL_JAAS_CONFIG,
                    "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${username}\" password=\"${password}\";"
            )
        })

        val sykepengeProducer = KafkaProducer<String, SykePengeVedtak>(Properties().apply {
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, configureAvroSerde<SykePengeVedtak>(mapOf(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to env.schemaRegistryUrl)).serializer().javaClass.name)
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
            put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, embeddedEnvironment.schemaRegistry!!.url)
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedEnvironment.brokersURL)
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
            put(SaslConfigs.SASL_MECHANISM, "PLAIN")
            put(
                    SaslConfigs.SASL_JAAS_CONFIG,
                    "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${username}\" password=\"${password}\";"
            )
        })

        val resultConsumer = KafkaConsumer<String, String>(Properties().apply {
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
            put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, embeddedEnvironment.schemaRegistry!!.url)
            put(ConsumerConfig.GROUP_ID_CONFIG, "vedtak.result.test")
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedEnvironment.brokersURL)
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
            put(SaslConfigs.SASL_MECHANISM, "PLAIN")
            put(
                    SaslConfigs.SASL_JAAS_CONFIG,
                    "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${username}\" password=\"${password}\";"
            )
        })

        val validator = Validator(env)
        validator.start()

        infotrygProducer.send(ProducerRecord<String, InfoTrygdVedtak>(Topics.VEDTAK_INFOTRYGD.name, "1", InfoTrygdVedtak("1", "123", "1.1.2018", "1.12.2018")))
        sykepengeProducer.send(ProducerRecord<String, SykePengeVedtak>(Topics.VEDTAK_SYKEPENGER.name, "1", SykePengeVedtak("1", "123", "1.1.2018", "1.12.2018")))

        resultConsumer.subscribe(listOf(Topics.VEDTAK_RESULTAT.name))

        val consumerRecords = resultConsumer.poll(Duration.ofSeconds(10))
        assertEquals(1, consumerRecords.count())
        validator.stop()
    }


}
