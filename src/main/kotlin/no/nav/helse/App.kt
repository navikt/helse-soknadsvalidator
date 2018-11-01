package no.nav.helse

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.prometheus.client.*
import io.prometheus.client.exporter.common.*
import kotlinx.coroutines.experimental.*
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.JoinWindows
import org.slf4j.*
import java.util.concurrent.TimeUnit

private val collectorRegistry: CollectorRegistry = CollectorRegistry.defaultRegistry
private val vedtakCounter = Counter.build().name("vedtak").help("Antall vedtak  vurdert").register()
private val korrektevedtakCounter = Counter.build().name("korrektevedtak").help("Antall vedtak som var riktig").register()
private val log = LoggerFactory.getLogger("no.nav.helse.App")


fun main(args: Array<String>) = runBlocking(block = {
    log.info("Starter validator")

    val builder = StreamsBuilder()


    val infotrygd = builder.stream<String, InfoTrygdVedtak>("vedtak.infotrygd")
    val sykepenger = builder.stream<String, SykePengeVedtak>("vedtak.sykepenger")
    val soknadOgVedtak = builder.stream<String, Vedtak>("vedtak.kombinert")


    sykepenger.join(infotrygd, vedtaksJoiner(), JoinWindows.of(TimeUnit.DAYS.toMillis(5))).to("vedtak.kombinert")

    soknadOgVedtak.foreach { _, vedtak -> if (vedtak.fasit.belop == vedtak.forslag.belop) korrektevedtakCounter.inc() }

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