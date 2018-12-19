package no.nav.helse

import io.prometheus.client.Counter
import no.nav.helse.streams.*
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.*
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class Validator(env: Environment) {

    private val appId = "spleis"
    private val log = LoggerFactory.getLogger("spleis")
    private val sykepengeKlient = SykepengeKlient(env)

    var streamConsumer: StreamConsumer

    init {
        streamConsumer = StreamConsumer(appId, KafkaStreams(setupTopology(),
                streamConfig(appId, env.bootstrapServersUrl, Pair(env.username, env.password), Pair(env.navTruststorePath, env.navTruststorePassword))))
    }

    fun start() {
        streamConsumer.start()
    }

    private val korrektevedtakCounter = Counter.build().name("korrektevedtak").help("antall korrekte vedtak").register()
    private val vedtakUtenInfotrygdCounter = Counter.build().name("for_tidlige_vedtak").help("antall vedtak fattet av vår løsning som ikke er fattet i infotrygd").register()
    private val vedtakMedInfotrygdCounter = Counter.build().name("matchede_vedtak").help("antall vedtak fattet av vår løsning der vi også har vedtak i infotrygd").register()
    private val vedtakCounter = Counter.build().name("vedtak").help("antall vedtak fattet av vår løsning").register()

    private fun setupTopology(): Topology {

        val builder = StreamsBuilder()

        val base: KStream<String, Pair<JSONObject, InfoTrygdVedtak?>> =builder.consumeTopic(Topics.VEDTAK_SYKEPENGER)
                .peek { _, value -> log.info("received.SP: " + value) }
                .peek { _, _ -> vedtakCounter.inc() }
                .mapValues { _, value -> VedtakToVedtakPairMapper(sykepengeKlient).apply(value) }

        val branches = base.branch(
                Predicate<String, Pair<JSONObject, InfoTrygdVedtak?>> { _, (_, infoTrygdVedtak) -> infoTrygdVedtak == null },
                Predicate<String, Pair<JSONObject, InfoTrygdVedtak?>> { _, (_, infoTrygdVedtak) -> infoTrygdVedtak != null }
        )

        // there was no infotrygd-thing
        branches[0].peek { _, _ -> vedtakUtenInfotrygdCounter.inc() }

        // there was an infotrygd-thing
        branches[1].peek { _, _ -> vedtakMedInfotrygdCounter.inc() }

        return builder.build()
    }

    fun vedtaksJoiner(): (JSONObject, JSONObject) -> JSONObject {
        return { fasit, forslag ->
            val vedtak = JSONObject()
            vedtak.put("fasit", fasit)
            vedtak.put("forslag", forslag)
            vedtak
        }
    }
}

class VedtakToVedtakPairMapper(val sykepengeKlient: SykepengeKlient) : ValueMapper<JSONObject, Pair<JSONObject, InfoTrygdVedtak?>> {
    override fun apply(value: JSONObject): Pair<JSONObject, InfoTrygdVedtak?> {
        return when (value.hasKeys(listOf("fom", "tom", "aktørId"))) {
            true -> Pair(value, sykepengeKlient.finnGjeldendeSykepengeVedtak(value.getString("aktørId"), value.getLocalDate("fom"), value.getLocalDate("tom")))
            false -> Pair(value, null)
        }
    }
}

fun JSONObject.getLocalDate(key: String): LocalDate {
    val rawValue: String = getString(key)
    return LocalDate.parse(rawValue, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
}

fun JSONObject.hasKeys(keys: List<String>): Boolean {
    return keys.any(this::has)
}