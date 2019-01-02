package no.nav.helse

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class SykepengeKlientTest {
    @Test
    fun `should translate from json to objects`() {
        val vedtak: Collection<InfoTrygdVedtak> = json.toByteArray().asInfoTrygdVedtakList()
        assertThat(vedtak).hasSize(2)
        assertThat(vedtak).containsOnly(targetVedtakOne, targetVedtakTwo)
    }

    @Test
    fun `should ignore corrupt entries`() {
        val vedtak: Collection<InfoTrygdVedtak> = corruptJson.toByteArray().asInfoTrygdVedtakList()
        assertThat(vedtak).hasSize(1)
        assertThat(vedtak).containsOnly(targetVedtakOne)
    }
}

val targetVedtakOne = InfoTrygdVedtak( fom = LocalDate.of(2001, 2, 1),
        tom = LocalDate.of(2001, 2, 28),
        grad = 10.0f,
        mottaker = "en slags aktør",
        beløp = BigDecimal("10000.11"))
val targetVedtakTwo = InfoTrygdVedtak( fom = LocalDate.of(2001, 3, 1),
        tom = LocalDate.of(2001, 3, 16),
        grad = 20.0f,
        mottaker = "en slags aktør",
        beløp = BigDecimal("20000.22"))
val json = """
    [
        {
            "fom": "2001-02-01",
            "tom": "2001-02-28",
            "grad": 10.0,
            "mottaker": "en slags aktør",
            "beløp": 10000.11
        },{
            "fom": "2001-03-01",
            "tom": "2001-03-16",
            "grad": 20.0,
            "mottaker": "en slags aktør",
            "beløp": 20000.22
        }
    ]
""".trimIndent()

val corruptJson = """
    [
        {
            "fom": "2001-02-01",
            "tom": "2001-02-28",
            "grad": 10.0,
            "mottaker": "en slags aktør",
            "beløp": 10000.11
        },{
            "fom": "03-01",
            "tom": "2001-03",
            "grad": "noe greier"
        }
    ]
""".trimIndent()