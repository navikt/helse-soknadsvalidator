package no.nav.helse.streams

data class Environment(
   val username: String = getRequiredEnvVar("SRVFILTER_PASSWORD"),
   val password: String = getRequiredEnvVar("SRVFILTER_USERNAME"),
   val bootstrapServersUrl: String = getRequiredEnvVar("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"),
   val schemaRegistryUrl: String = getRequiredEnvVar("KAFKA_SCHEMA_REGISTRY_URL", "localhost:8081"),
   val httpPort: Int? = null,
   val navTruststorePath: String? = getEnvVar("NAV_TRUSTSTORE_PATH"),
   val navTruststorePassword: String? = getEnvVar("NAV_TRUSTSTORE_PASSWORD"),
   val stateDir: String? = getEnvVar("STATE_DIR")
   )

private fun getRequiredEnvVar(varName: String, defaultValue: String? = null) =
   getEnvVar(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")

private fun getEnvVar(varName: String) = System.getenv(varName)
