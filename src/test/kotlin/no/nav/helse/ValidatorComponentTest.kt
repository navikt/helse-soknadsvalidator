package no.nav.helse

import io.prometheus.client.CollectorRegistry
import no.nav.common.JAASCredential
import no.nav.common.KafkaEnvironment
import no.nav.helse.streams.Topics
import org.apache.kafka.clients.producer.ProducerRecord
import org.json.JSONObject
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.Duration

class ValidatorComponentTest {

    companion object {
        private const val username = "srvkafkaclient"
        private const val password = "kafkaclient"

        val embeddedEnvironment = KafkaEnvironment(
                users = listOf(JAASCredential(username, password)),
                autoStart = false,
                withSchemaRegistry = true,
                withSecurity = true,
                topics = listOf(Topics.VEDTAK_INFOTRYGD.name, Topics.VEDTAK_SYKEPENGER.name, Topics.VEDTAK_RESULTAT.name, Topics.VEDTAK_KOMBINERT.name)
        )

        val env = Environment(
                username = username,
                password = password,
                bootstrapServersUrl = embeddedEnvironment.brokersURL
                //schemaRegistryUrl = embeddedEnvironment.schemaRegistry!!.url
        )

        val validator = Validator(env)

        @BeforeAll
        @JvmStatic
        fun setup() {
            CollectorRegistry.defaultRegistry.clear()
            embeddedEnvironment.start()
            validator.start()
        }

        @AfterAll
        @JvmStatic
        fun teardown() {
            validator.streamConsumer?.stop()
            CollectorRegistry.defaultRegistry.clear()
            embeddedEnvironment.tearDown()
        }
    }


    @Test
    fun ` embedded kafka cluster is up and running `() {
        assertEquals(embeddedEnvironment.serverPark.status, KafkaEnvironment.ServerParkStatus.Started)
    }


    //@Test
    // FIXME: At some point, when the skeleton gets its flesh on
    fun ` two vedtak with same key and equal amounts are counted`() {

        val infotrygProducer = infotrygdProducer(env)
        val sykepengeProducer = sykePengeProducer(env)
        val resultConsumer = resultatConsumer(env)

        infotrygProducer.send(ProducerRecord<String, JSONObject>(Topics.VEDTAK_INFOTRYGD.name, "3", infoTrygdVedtak()))
        sykepengeProducer.send(ProducerRecord<String, JSONObject>(Topics.VEDTAK_SYKEPENGER.name, "2", sykepengeVedtak()))
        sykepengeProducer.send(ProducerRecord<String, JSONObject>(Topics.VEDTAK_SYKEPENGER.name, "3", sykepengeVedtak()))

        resultConsumer.subscribe(listOf(Topics.VEDTAK_RESULTAT.name))

        val consumerRecords = resultConsumer.poll(Duration.ofSeconds(10))
        assertEquals(1, consumerRecords.count())
    }

    fun infoTrygdVedtak(): JSONObject {
        val json = JSONObject()
        return json.put("id", "3").put("belop", "123").put("fom", "1.1.2018").put("tom", "1.1.2018")
    }

    fun sykepengeVedtak(): JSONObject {
        val json = JSONObject()
        return json.put("id", "3").put("belop", "123").put("fom", "1.1.2018").put("tom", "1.1.2018")
    }
}
