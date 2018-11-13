package no.nav.helse

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.prometheus.client.Counter
import no.nav.helse.avro.InfoTrygdVedtak
import no.nav.helse.avro.SykePengeVedtak
import no.nav.helse.avro.Vedtak
import no.nav.helse.streams.*
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.JoinWindows
import org.apache.kafka.streams.kstream.Joined
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit


class Validator(val env: Environment) {

    private val appId = "sykepengevedtak-validator"
    private val log = LoggerFactory.getLogger("vedtakvalidator")

    private val streamConsumer = StreamConsumer(appId, env, valider())

    fun start() {
        streamConsumer.start()
    }

    fun stop() {
        streamConsumer.stop()
    }

    private val korrektevedtakCounter = Counter.build().name("korrektevedtak").help("antall korrekte vedtak").register()
    private val vedtakCounter = Counter.build().name("vedtak").help("antall  vedtak").register()

    private fun valider(): KafkaStreams {

        val builder = StreamsBuilder()

        val sykePengeStream = builder.consumeTopic(Topics.VEDTAK_SYKEPENGER.copy(
                valueSerde = sykepengeSerde()))
                .peek { _, value -> log.info("received.SP: " + value) }


        builder.consumeTopic(Topics.VEDTAK_INFOTRYGD.copy(
                valueSerde = infotrygdSerde()))
                .peek { _, value -> log.info("received.IT: " + value) }
                .join(sykePengeStream, vedtaksJoiner(), joinWindows(), joined())
                .toTopic(Topics.VEDTAK_KOMBINERT.copy(
                        valueSerde = kombinertSerde()))

        builder.consumeTopic(Topics.VEDTAK_KOMBINERT.copy(
                valueSerde = kombinertSerde()))
                .peek { _, value -> log.info("received.KOMBINERT: " + value) }
                .filter { _, vedtak -> vedtak.getFasit().getBelop() == vedtak.getForslag().getBelop() }
                .mapValues { value -> value.getFasit().getBelop() }
                .peek { _, _ -> korrektevedtakCounter.inc() }
                .toTopic(Topics.VEDTAK_RESULTAT)


        return KafkaStreams(builder.build(), streamConfig(appId, env))
    }



    private fun joined(): Joined<String, InfoTrygdVedtak, SykePengeVedtak>? {
        return Joined.with(Serdes.String(), infotrygdSerde(), sykepengeSerde())
    }

    private fun kombinertSerde(): SpecificAvroSerde<Vedtak> {
        return configureAvroSerde(mapOf(
                KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG to env.schemaRegistryUrl

        ))
    }

    private fun sykepengeSerde(): SpecificAvroSerde<SykePengeVedtak> {
        return configureAvroSerde(
                mapOf(
                        AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to env.schemaRegistryUrl

                )
        )
    }

    private fun infotrygdSerde(): SpecificAvroSerde<InfoTrygdVedtak> {
        return configureAvroSerde(
                mapOf(
                        KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG to env.schemaRegistryUrl

                ))
    }

    private fun joinWindows() = JoinWindows.of(TimeUnit.MINUTES.toMillis(5))

    private fun vedtaksJoiner(): (InfoTrygdVedtak, SykePengeVedtak) -> Vedtak {
        return { fasit, forslag ->
            log.info("joining messages: " + fasit + forslag)
            vedtakCounter.inc()
            Vedtak.newBuilder().setFasit(fasit).setForslag(forslag).build()
        }
    }
}
