package no.nav.helse

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.common.JAASCredential
import no.nav.common.KafkaEnvironment
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals

class ValidatorComponentTest {

    companion object {
        private const val username = "srvkafkaclient"
        private const val password = "kafkaclient"

        val embeddedEnvironment = KafkaEnvironment(
                users = listOf(JAASCredential(username, password)),
                autoStart = false,
                withSchemaRegistry = true,
                withSecurity = false,
                topics = listOf("vedtak.infotrygd", "vedtak.sykepenger")
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

    private val serdeConfig = mapOf(
            AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to
                    (System.getenv("KAFKA_SCHEMA_REGISTRY_URL") ?: "http://localhost:8081")
    )

    @Test
    fun ` embedded kafka cluster is up and running `() {
        assertEquals(embeddedEnvironment.serverPark.status, KafkaEnvironment.ServerParkStatus.Started)
    }

    @Test
    fun ` the very first test`() {
        assertEquals(embeddedEnvironment.serverPark.status, KafkaEnvironment.ServerParkStatus.Started)
    }

    @Test
    fun ` producing a message `() {
        val producer = KafkaProducer<String, Vedtak>(Properties().apply {
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, configureAvroSerde<Vedtak>(serdeConfig))
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
            put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, embeddedEnvironment.schemaRegistry!!.url)
            put(ConsumerConfig.GROUP_ID_CONFIG, "dummy-dagpenger-innkomne-jp")
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedEnvironment.brokersURL)
        })
        producer.send(ProducerRecord<String, Vedtak>("vedtak.infostrygd", Vedtak(InfoTrygdVedtak("string", 123, Date(), Date()), SykePengeVedtak("", 123, Date(), Date()))))
    }


    fun <T : SpecificRecord?> configureAvroSerde(serdeConfig: Map<String, Any>): SpecificAvroSerde<T> {
        return SpecificAvroSerde<T>().apply { configure(serdeConfig, false) }
    }

}
