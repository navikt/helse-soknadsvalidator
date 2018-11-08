package no.nav.helse.streams

import org.apache.kafka.clients.*
import org.apache.kafka.clients.consumer.*
import org.apache.kafka.common.config.*
import org.apache.kafka.streams.*
import org.apache.kafka.streams.errors.*
import org.slf4j.*
import java.io.*
import java.util.*

private val log = LoggerFactory.getLogger("StreamConfig")

fun streamConfig(
   appId: String,
   env: Environment
): Properties = Properties().apply {
      put(CommonClientConfigs.RETRY_BACKOFF_MS_CONFIG, 1000)
      put(CommonClientConfigs.RECONNECT_BACKOFF_MS_CONFIG , 5000)
      put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, env.bootstrapServersUrl)
      put(StreamsConfig.APPLICATION_ID_CONFIG, appId)
      // TODO Using processing guarantee requires replication of 3, not possible with current single node dev environment
      //put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, "exactly_once"),
      put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1)
      put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
      put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndFailExceptionHandler::class.java)

      env.stateDir?.let { put(StreamsConfig.STATE_DIR_CONFIG, env.stateDir) }

      KafkaCredential(env.username, env.password).let { credential ->
         log.info("Using user name ${credential.username} to authenticate against Kafka brokers ")
         put(SaslConfigs.SASL_MECHANISM, "PLAIN")
         put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
         put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${credential.username}\" password=\"${credential.password}\";")

         env.navTruststorePath?.let {
            try {
               put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL")
               put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, File(it).absolutePath)
               put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, env.navTruststorePassword)
               log.info("Configured '${SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG}' location ")
            } catch (ex: Exception) {
               log.error("Failed to set '${SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG}' location", ex)
            }
         }
      }
   }

