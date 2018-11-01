package no.nav.helse

import java.util.*

data class InfoTrygdVedtak(
        val id: String,
        val belop: Long,
        val fraOgMed: Date,
        val tilOgMed: Date
)

data class SykePengeVedtak(
        val id: String,
        val belop: Long,
        val fraOgMed: Date,
        val tilOgMed: Date
)


class Vedtak(
        val fasit: InfoTrygdVedtak,
        val forslag: SykePengeVedtak
)