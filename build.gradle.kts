import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

val kotlin_version: String by extra


val junitJupiterVersion = "5.3.1"
val spekVersion = "1.2.1"
val kluentVersion = "1.41"
val slf4jVersion = "1.7.25"
val ktorVersion = "0.9.3"
val prometheusVersion = "0.5.0"
val kafkaVersion = "2.0.0"
val confluentVersion = "5.0.0"
val avroVersion = "1.8.2"


val mainClass = "no.nav.helse.AppKt"

plugins {
    application
    id("org.jetbrains.kotlin.jvm") version "1.3.0"
    id("com.github.johnrengelman.shadow") version "2.0.0"
    id("com.commercehub.gradle.plugin.avro") version "0.14.2"

}
apply {
    plugin("kotlin")
}

buildscript {
    //var kotlin_version: String by extra
    //kotlin_version = "1.3.0"
    dependencies {
        classpath("org.junit.platform:junit-platform-gradle-plugin:1.2.0")
    }
}

kotlin.experimental.coroutines = Coroutines.ENABLE

application {
    mainClassName = "$mainClass"
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile("com.ibm.mq:com.ibm.mq.allclient:9.1.0.0")
    compile("javax.jms:javax.jms-api:2.0.1")
    compile("org.slf4j:slf4j-simple:$slf4jVersion")
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-core:0.30.2")
    compile("io.ktor:ktor-server-netty:$ktorVersion")
    compile("io.prometheus:simpleclient_common:$prometheusVersion")
    compile("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    compile("org.apache.kafka:kafka-clients:$kafkaVersion")
    compile("org.apache.kafka:kafka-streams:$kafkaVersion")
    compile("io.confluent:kafka-streams-avro-serde:$confluentVersion")
    api("org.apache.avro:avro:$avroVersion")



    testCompile("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
    testCompile("org.amshove.kluent:kluent:$kluentVersion")
    testImplementation ("no.nav:kafka-embedded-env:2.0.1")

    testCompile("org.jetbrains.spek:spek-api:$spekVersion") {
        exclude(group = "org.jetbrains.kotlin")
    }
    testRuntime("org.jetbrains.spek:spek-junit-platform-engine:$spekVersion") {
        exclude(group = "org.junit.platform")
        exclude(group = "org.jetbrains.kotlin")
    }
}

repositories {
    maven("https://dl.bintray.com/kotlin/ktor")
    maven("http://packages.confluent.io/maven/")
    maven("https://repo.adeo.no/repository/maven-central")
    jcenter()
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}


tasks.withType<Wrapper> {
    gradleVersion = "4.10.2"
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}