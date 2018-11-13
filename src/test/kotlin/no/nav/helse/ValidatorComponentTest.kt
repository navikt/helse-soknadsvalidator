package no.nav.helse

import no.nav.common.JAASCredential
import no.nav.common.KafkaEnvironment
import no.nav.helse.avro.InfoTrygdVedtak
import no.nav.helse.avro.SykePengeVedtak
import no.nav.helse.streams.Environment
import no.nav.helse.streams.Topics
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import java.time.Duration
import kotlin.test.assertEquals

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
                bootstrapServersUrl = embeddedEnvironment.brokersURL,
                schemaRegistryUrl = embeddedEnvironment.schemaRegistry!!.url
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

    }


    @Test
    fun ` two vedtak with same key and equal amounts are counted`() {

        val infotrygProducer = infotrygdProducer(env)
        val sykepengeProducer = sykePengeProducer(env)
        val resultConsumer = resultatConsumer(env)


        val validator = Validator(env)
        validator.start()

        infotrygProducer.send(ProducerRecord<String, InfoTrygdVedtak>(Topics.VEDTAK_INFOTRYGD.name, "3", InfoTrygdVedtak("3", "123", "1.1.2018", "1.12.2018")))
        sykepengeProducer.send(ProducerRecord<String, SykePengeVedtak>(Topics.VEDTAK_SYKEPENGER.name, "2", SykePengeVedtak("2", "123", "1.1.2018", "1.12.2018")))
        sykepengeProducer.send(ProducerRecord<String, SykePengeVedtak>(Topics.VEDTAK_SYKEPENGER.name, "3", SykePengeVedtak("3", "123", "1.1.2018", "1.12.2018")))


        resultConsumer.subscribe(listOf(Topics.VEDTAK_RESULTAT.name))

        val consumerRecords = resultConsumer.poll(Duration.ofSeconds(10))
        assertEquals(1, consumerRecords.count())
        validator.stop()
    }


}
