
val kotlin_version: String by extra

val junitJupiterVersion = "5.3.1"
val spekVersion = "1.2.1"
val kluentVersion = "1.41"
val slf4jVersion = "1.7.25"
val ktorVersion = "1.0.0-beta-3"
val prometheusVersion = "0.5.0"
val kafkaVersion = "2.0.0"
val confluentVersion = "5.0.0"
val avroVersion = "1.8.2"
val orgJsonVersion = "20180813"



val mainClass = "no.nav.helse.AppKt"

plugins {
    application
    id("com.commercehub.gradle.plugin.avro") version "0.9.1"
    kotlin("jvm") version "1.3.0"
    id("com.github.johnrengelman.shadow") version "2.0.0"

}

buildscript {
    dependencies {
        classpath("org.junit.platform:junit-platform-gradle-plugin:1.2.0")
    }
}

application {
    mainClassName = "$mainClass"
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile("com.ibm.mq:com.ibm.mq.allclient:9.1.0.0")
    compile("org.slf4j:slf4j-simple:$slf4jVersion")
    compile("io.ktor:ktor-server-netty:$ktorVersion")
    compile("org.apache.avro:avro:$avroVersion")

    implementation("io.prometheus:simpleclient_common:$prometheusVersion")
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")

    compile("org.apache.kafka:kafka-clients:$kafkaVersion")
    compile("org.apache.kafka:kafka-streams:$kafkaVersion")
    compile("io.confluent:kafka-streams-avro-serde:$confluentVersion")

    api("org.json:json:$orgJsonVersion")



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

    testImplementation("no.nav:kafka-embedded-env:2.0.1")
}

repositories {
    maven("https://dl.bintray.com/kotlin/ktor")
    maven("http://packages.confluent.io/maven/")
    maven("https://repo.adeo.no/repository/maven-central")
    jcenter()
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_10
    targetCompatibility = JavaVersion.VERSION_1_10
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
