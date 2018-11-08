package no.nav.helse.streams

import org.apache.kafka.common.serialization.*

data class Topic<K, V>(
   val name: String,
   val keySerde: Serde<K>,
   val valueSerde: Serde<V>
)
