package no.nav.helse

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.json.JSONObject
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

object ValidatorFeature: Spek({

    context("joins two vedtak, one representing the vedtak from infotrygd, the other from sykepenger") {
        given("two JSON objects") {

            val infotrygdVedtak = JSONObject()
                    .put("id", "3")
                    .put("belop", "1230")
                    .put("fom", "1.1.2018")
                    .put("tom", "1.1.2018")

            val sykepengeVedtak = JSONObject()
                    .put("id", "3")
                    .put("belop", "123")
                    .put("fom", "1.1.2018")
                    .put("tom", "1.1.2018")

            on("joining") {
                var result = Validator().vedtaksJoiner()(infotrygdVedtak, sykepengeVedtak)

                it("returns a JSON object containing both objects") {
                    shouldEqualJSON(result.getJSONObject("fasit"), infotrygdVedtak)
                    shouldEqualJSON(result.getJSONObject("forslag"), sykepengeVedtak)
                }
            }
        }
    }
})

private fun shouldEqualJSON (json1:JSONObject, json2: JSONObject){
    JSONAssert.assertEquals(json1, json2, JSONCompareMode.LENIENT)
}
