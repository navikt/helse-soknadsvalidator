package no.nav.helse

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import java.math.BigInteger
import java.time.LocalDate

class SykepengeKlient(val env: Environment) {

    fun finnGjeldendeSykepengeVedtak(aktørId: String, fom: LocalDate, tom: LocalDate):InfoTrygdVedtak? {
        val (_: Request, _: Response, result: Result<ByteArray, FuelError>) = "${env.sparkelUrl}/api/sykepengeListe/$aktørId?fom=${fom}&tom=${tom}".httpGet().response()
        return result.component1()?.asInfoTrygdVedtak()
    }
}

data class InfoTrygdVedtak(val fom: LocalDate,
                           val tom: LocalDate,
                           val grad: Float,
                           val beløp: BigInteger,
                           val mottaker: String)

fun ByteArray.asInfoTrygdVedtak(): InfoTrygdVedtak? {
    return null
}