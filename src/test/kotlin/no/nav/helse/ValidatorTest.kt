package no.nav.helse


import org.json.JSONObject
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

class ValidatorTest {


    @Test
    fun ` joins vedtak `() {

        val result = Validator().vedtaksJoiner()(
              infoTrygdVedtak(), sykepengeVedtak()
        )

        assertEqualJson(result.getJSONObject("fasit"), infoTrygdVedtak())
        assertEqualJson(result.getJSONObject("forslag"), sykepengeVedtak())
    }


    private fun infoTrygdVedtak(): JSONObject {
        return JSONObject()
                .put("id", "3")
                .put("belop", "1230")
                .put("fom", "1.1.2018")
                .put("tom", "1.1.2018")
    }

    private fun sykepengeVedtak(): JSONObject {
        return JSONObject().
                put("id", "3")
                .put("belop", "123")
                .put("fom", "1.1.2018")
                .put("tom", "1.1.2018")
    }

    private fun assertEqualJson(json1:JSONObject, json2: JSONObject){
        JSONAssert.assertEquals(json1, json2, JSONCompareMode.LENIENT)
    }
}
