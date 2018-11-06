package no.nav.helse

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondWrite
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter
import io.prometheus.client.exporter.common.TextFormat
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.JoinWindows
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

private val collectorRegistry: CollectorRegistry = CollectorRegistry.defaultRegistry
private val vedtakCounter = Counter.build().name("vedtak").help("Antall vedtak  vurdert").register()
private val korrektevedtakCounter = Counter.build().name("korrektevedtak").help("Antall vedtak som var riktig").register()
private val log = LoggerFactory.getLogger("no.nav.helse.App")


fun main(args: Array<String>) = run(block = {
    log.info("Starter validator")

    val builder = StreamsBuilder()


    val infotrygd = builder.stream<String, InfoTrygdVedtak>("vedtak.infotrygd")
    val sykepenger = builder.stream<String, SykePengeVedtak>("vedtak.sykepenger")
    val soknadOgVedtak = builder.stream<String, Vedtak>("vedtak.kombinert")
    val resultat = builder.stream<String, String>("vedtak.resultat")


    sykepenger.join(infotrygd, vedtaksJoiner(), JoinWindows.of(TimeUnit.DAYS.toMillis(5))).to("vedtak.kombinert")

    soknadOgVedtak.filter({ _, vedtak -> vedtak.fasit.belop == vedtak.forslag.belop })
            .mapValues ({ value -> value.fasit.belop.toString()})
    .to("vedtak.resultat")

    resultat.foreach { key, value -> korrektevedtakCounter.inc() }


    startWebserver()
})

private fun vedtaksJoiner(): (SykePengeVedtak, InfoTrygdVedtak) -> Vedtak {
    return { forslag, fasit ->
        vedtakCounter.inc()
        Vedtak(fasit, forslag)
    }
}


private fun startWebserver() {
    embeddedServer(Netty, 8080) {
        routing {
            get("/isalive") {
                call.respond(HttpStatusCode.OK)
            }
            get("/isready") {
                call.respond(HttpStatusCode.OK)
            }
            get("/metrics") {
                val names = call.request.queryParameters.getAll("name[]")?.toSet() ?: setOf()
                call.respondWrite(ContentType.parse(TextFormat.CONTENT_TYPE_004)) {
                    TextFormat.write004(this, collectorRegistry.filteredMetricFamilySamples(names))
                }
            }
        }
    }.start(wait = true)
}