package no.nav.helse

import no.nav.helse.streams.Environment
import no.nav.helse.streams.StreamConsumer
import no.nav.helse.avro.*
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.JoinWindows
import org.slf4j.LoggerFactory


class Validator {

    private val appId = "sykepengevedtak-validator"
    private val env: Environment = Environment()
    private val log = LoggerFactory.getLogger("vedtakvalidator")

    fun start() {
        StreamConsumer(appId, Environment(), valider()).start()
    }

    private fun valider(): KafkaStreams {

        val builder = StreamsBuilder()


      //  val infotrygd = builder.stream<String, InfoTrygdVedtak>("vedtak.infotrygd")
      //  val sykepenger = builder.stream<String, SykePengeVedtak>("vedtak.sykepenger")
      //  val soknadOgVedtak = builder.stream<String, Vedtak>("vedtak.kombinert")
      //  val resultat = builder.stream<String, String>("vedtak.resultat")


        builder.consumeTopic("vedtak.infotrygd").join(infotrygd, vedtaksJoiner(), JoinWindows.of(TimeUnit.DAYS.toMillis(5))).to("vedtak.kombinert")

        soknadOgVedtak.filter({ _, vedtak -> vedtak.getFasit().getBelop() == vedtak.getForslag().getBelop() })
                .mapValues({ value -> value.getFasit().getBelop() })
                .to("vedtak.resultat")

        resultat.foreach { _, _ -> korrektevedtakCounter.inc() }
    }

    private fun vedtaksJoiner(): (SykePengeVedtak, InfoTrygdVedtak) -> Vedtak {
        return { forslag, fasit ->
            vedtakCounter.inc()
            Vedtak(fasit, forslag)
        }
    }
}
