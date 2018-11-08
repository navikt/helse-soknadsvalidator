package no.nav.helse

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
        StreamConsumer(appId, Environment(), valider()).start()
    }

    private val korrektevedtakCounter = Counter.build().name("korrektevedtak").help("antall korrekte vedtak").register()
    private val vedtakCounter = Counter.build().name("vedtak").help("antall  vedtak").register()

    private fun valider(): KafkaStreams {

        val builder = StreamsBuilder()

        val vedtakSykepenger = builder.consumeGenericTopic(Topics.VEDTAK_SYKEPENGER)

        builder.consumeTopic(Topics.VEDTAK_INFOTRYGD).join(vedtakSykepenger, vedtaksJoiner(), JoinWindows.of(TimeUnit.DAYS.toMillis(5))).toTopic(Topics.VEDTAK_KOMBINERT)

        builder.consumeTopic(Topics.VEDTAK_KOMBINERT).filter { _, vedtak -> vedtak.getFasit().getBelop() == vedtak.getForslag().getBelop() }
                .mapValues { value -> value.getFasit().getBelop() }
                .to(Topics.VEDTAK_RESULTAT.name)

        builder.consumeGenericTopic(Topics.VEDTAK_RESULTAT).foreach { _, _ -> korrektevedtakCounter.inc() }

        return KafkaStreams(builder.build(), streamConfig(appId, env))
    }

    private fun vedtaksJoiner(): (InfoTrygdVedtak, SykePengeVedtak) -> Vedtak {
        return { fasit, forslag ->
            vedtakCounter.inc()
            Vedtak(fasit, forslag)
        }
    }
}
