package no.nav.helse

data class Environment(
        val username: String? = getEnvVar("KAFKA_USERNAME"),
        val password: String? = getEnvVar("KAFKA_PASSWORD"),
        val bootstrapServersUrl: String = getEnvVar("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"),
        val schemaRegistryUrl: String = getEnvVar("KAFKA_SCHEMA_REGISTRY_URL", "localhost:8081"),
        val httpPort: Int? = null,
        val navTruststorePath: String? = getEnvVar("NAV_TRUSTSTORE_PATH"),
        val navTruststorePassword: String? = getEnvVar("NAV_TRUSTSTORE_PASSWORD"),
        val stsBaseUrl: String = getEnvVar("STS_BASE_URL", "http://security-token-service"),
        val sparkelUrl: String = getEnvVar("SPARKEL_BASE_URL", "http://localhost:8080")
)

private fun getEnvVar(varName: String, defaultValue: String? = null) =
        getEnvVar(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")

private fun getEnvVar(varName: String) = System.getenv(varName)
