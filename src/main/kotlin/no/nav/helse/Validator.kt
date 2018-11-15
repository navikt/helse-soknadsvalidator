package no.nav.helse

import io.prometheus.client.Counter
import no.nav.helse.streams.*
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.JoinWindows
import org.apache.kafka.streams.kstream.Joined
import org.json.JSONObject
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

        val sykePengeStream = builder.consumeTopic(Topics.VEDTAK_SYKEPENGER)
                .peek { _, value -> log.info("received.SP: " + value) }


        builder.consumeTopic(Topics.VEDTAK_INFOTRYGD)
                .peek { _, value -> log.info("received.IT: " + value) }
                .join(sykePengeStream, vedtaksJoiner(), fiveMin(), joinDeserializor())
                .toTopic(Topics.VEDTAK_KOMBINERT)

        builder.consumeTopic(Topics.VEDTAK_KOMBINERT)
                .peek { _, value -> log.info("received.KOMBINERT: " + value) }
                .filter { _, vedtak -> vedtak.getJSONObject("fasit").getString("belop") == vedtak.getJSONObject("forslag").getString("belop") }
                .mapValues { value -> value.getJSONObject("fasit").getString("belop") }
                .peek { _, _ -> korrektevedtakCounter.inc() }
                .toTopic(Topics.VEDTAK_RESULTAT)


        return KafkaStreams(builder.build(), streamConfig(appId, env))
    }


      private fun joinDeserializor(): Joined<String, JSONObject, JSONObject> {
          return Joined.with(Serdes.String(), Topics.VEDTAK_INFOTRYGD.valueSerde, Topics.VEDTAK_SYKEPENGER.valueSerde)
      }


    private fun fiveMin() = JoinWindows.of(TimeUnit.MINUTES.toMillis(5))

    private fun vedtaksJoiner(): (JSONObject, JSONObject) -> JSONObject {
        return { fasit, forslag ->
            log.info("joining messages: " + fasit + forslag)
            vedtakCounter.inc()
            val vedtak = JSONObject()
            vedtak.put("fasit", fasit)
            vedtak.put("forslag", forslag)
            vedtak
        }
    }
}
