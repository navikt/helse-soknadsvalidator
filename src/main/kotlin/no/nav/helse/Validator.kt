package no.nav.helse

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.prometheus.client.Counter
import no.nav.helse.avro.InfoTrygdVedtak
import no.nav.helse.avro.SykePengeVedtak
import no.nav.helse.avro.Vedtak
import no.nav.helse.streams.*
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.JoinWindows
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit


class Validator(val env: Environment) {

    private val appId = "sykepengevedtak-validator"
    //private val env: Environment = Environment()
    private val log = LoggerFactory.getLogger("vedtakvalidator")

    fun start() {
        StreamConsumer(appId, env, valider()).start()
    }

    private val korrektevedtakCounter = Counter.build().name("korrektevedtak").help("antall korrekte vedtak").register()
    private val vedtakCounter = Counter.build().name("vedtak").help("antall  vedtak").register()

    private fun valider(): KafkaStreams {

        val builder = StreamsBuilder()

        //val vedtakSykepenger =

        val sykePengeStream = builder.consumeTopic(Topics.VEDTAK_SYKEPENGER.copy(
                valueSerde = configureAvroSerde(
                        mapOf(
                                KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG to env.schemaRegistryUrl,
                                KafkaAvroDeserializerConfig.AUTO_REGISTER_SCHEMAS to true,
                                KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG to true
                        )
                ))).peek { _, value -> log.info("recieved.SP: " + value) }
        builder.consumeTopic(Topics.VEDTAK_INFOTRYGD.copy(
                valueSerde = configureAvroSerde(
                        mapOf(
                                KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG to env.schemaRegistryUrl,
                                KafkaAvroDeserializerConfig.AUTO_REGISTER_SCHEMAS to true,
                                KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG to true
                        )                ))).peek { _, value -> log.info("recieved.IT: " + value) }.join(sykePengeStream, vedtaksJoiner(), JoinWindows.of(TimeUnit.MINUTES.toMillis(5))).toTopic(Topics.VEDTAK_KOMBINERT)

        builder.consumeTopic(Topics.VEDTAK_KOMBINERT.copy(
                valueSerde = configureAvroSerde(
                        mapOf(
                                KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG to env.schemaRegistryUrl,
                                KafkaAvroDeserializerConfig.AUTO_REGISTER_SCHEMAS to true,
                                KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG to true
                        )                ))).peek { _, value -> log.info("recieved.KOMBINERT: " + value) }.filter { _, vedtak -> vedtak.getFasit().getBelop() == vedtak.getForslag().getBelop() }
                .mapValues { value -> value.getFasit().getBelop() }
                .to(Topics.VEDTAK_RESULTAT.name)

        builder.consumeGenericTopic(Topics.VEDTAK_RESULTAT).foreach { _, _ -> korrektevedtakCounter.inc() }

        return KafkaStreams(builder.build(), streamConfig(appId, env))
    }

    private fun vedtaksJoiner(): (InfoTrygdVedtak, SykePengeVedtak) -> Vedtak {
        return { fasit, forslag ->
            log.info("joingin messages: " + fasit + forslag)
            vedtakCounter.inc()
            Vedtak.newBuilder().setFasit(fasit).setForslag(forslag).build()
        }
    }
}
