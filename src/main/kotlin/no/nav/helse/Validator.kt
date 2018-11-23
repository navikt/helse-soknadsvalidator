package no.nav.helse

import io.prometheus.client.Counter
import no.nav.helse.streams.*
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.JoinWindows
import org.apache.kafka.streams.kstream.Joined
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit


class Validator() {


    private val appId = "sykepengevedtak-validator"
    private val log = LoggerFactory.getLogger("sykepengevedtak-validator")

    var streamConsumer: StreamConsumer? = null

    constructor(env: Environment) : this() {
        streamConsumer = StreamConsumer(appId, KafkaStreams(setupTopology(),
                streamConfig(appId, env.bootstrapServersUrl, Pair(env.username, env.password), Pair(env.navTruststorePath, env.navTruststorePassword))))
    }

    fun start() {
        streamConsumer?.start()
    }


    private val korrektevedtakCounter = Counter.build().name("korrektevedtak").help("antall korrekte vedtak").register()
    private val vedtakCounter = Counter.build().name("vedtak").help("antall  vedtak").register()

    private fun setupTopology(): Topology {

        val builder = StreamsBuilder()

        val sykePengeStream = builder.consumeTopic(Topics.VEDTAK_SYKEPENGER)
                .peek { _, value -> log.info("received.SP: " + value) }

        builder.consumeTopic(Topics.VEDTAK_INFOTRYGD)
                .peek { _, value -> log.info("received.IT: " + value) }
                .join(sykePengeStream, vedtaksJoiner(), fiveMin(), joinDeserializor())
                .peek { _, _ -> vedtakCounter.inc() }
                .toTopic(Topics.VEDTAK_KOMBINERT)

        builder.consumeTopic(Topics.VEDTAK_KOMBINERT)
                .peek { _, value -> log.info("received.KOMBINERT: " + value) }
                .filter { _, vedtak -> vedtak.getJSONObject("fasit").getString("belop") == vedtak.getJSONObject("forslag").getString("belop") }
                .mapValues { value -> value.getJSONObject("fasit").getString("belop") }
                .peek { _, _ -> korrektevedtakCounter.inc() }
                .toTopic(Topics.VEDTAK_RESULTAT)

        return builder.build()
    }

    private fun joinDeserializor(): Joined<String, JSONObject, JSONObject> {
        return Joined.with(Serdes.String(), Topics.VEDTAK_INFOTRYGD.valueSerde, Topics.VEDTAK_SYKEPENGER.valueSerde)
    }


    private fun fiveMin() = JoinWindows.of(TimeUnit.MINUTES.toMillis(5))

    fun vedtaksJoiner(): (JSONObject, JSONObject) -> JSONObject {
        return { fasit, forslag ->
            val vedtak = JSONObject()
            vedtak.put("fasit", fasit)
            vedtak.put("forslag", forslag)
            vedtak
        }
    }
}
