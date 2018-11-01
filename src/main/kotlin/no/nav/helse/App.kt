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
import org.slf4j.*

private val collectorRegistry: CollectorRegistry = CollectorRegistry.defaultRegistry
private val log = LoggerFactory.getLogger("no.nav.helse.App")


fun main(args: Array<String>) = runBlocking {
    log.info("Starter validator")

    startWebserver()
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