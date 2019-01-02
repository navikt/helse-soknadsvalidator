package no.nav.helse

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDate

val log: Logger = LoggerFactory.getLogger("spleis-sykepengeklient")

class SykepengeKlient(val env: Environment) {

    fun finnGjeldendeSykepengeVedtak(aktørId: String, fom: LocalDate, tom: LocalDate): InfoTrygdVedtak? {
        val (_: Request, _: Response, result: Result<ByteArray, FuelError>) = "${env.sparkelUrl}/api/sykepengeListe/$aktørId?fom=${fom}&tom=${tom}".httpGet().response()
        return result.component1()
                ?.asInfoTrygdVedtakList()
                ?.filter { it.fom == fom && it.tom == tom }
                ?.firstOrNull()
    }
}

data class InfoTrygdVedtak(val fom: LocalDate,
                           val tom: LocalDate,
                           val grad: Float,
                           val mottaker: String,
                           val beløp: BigDecimal)

fun ByteArray.asInfoTrygdVedtakList(): List<InfoTrygdVedtak> {
    try {
        val json = JSONArray(String(this))
        if (json.isEmpty) return emptyList()

        return json.map {
            when (it) {
                is JSONObject -> it.asInfotrygdVedtak()
                else -> null
            }
        }
                .filter { it != null }
                .requireNoNulls()
    } catch (e: JSONException) {
        log.warn("Could not parse response from sparkel-sykepenger as a json array")
        return emptyList()
    }
}

private fun JSONObject.asInfotrygdVedtak(): InfoTrygdVedtak? {
    return if (this.hasKeys(listOf("fom", "tom", "grad", "mottaker", "beløp")))
        try {
            InfoTrygdVedtak(fom = this.getLocalDate("fom"),
                    tom = this.getLocalDate("tom"),
                    grad = this.getFloat("grad"),
                    mottaker = this.getString("mottaker"),
                    beløp = this.getBigDecimal("beløp"))
        } catch (e: JSONException) {
            log.warn("Could not parse vedtak", e)
            null
        }
    else {
        log.warn("Could not parse vedtak due to missing fields")
        null
    }
}