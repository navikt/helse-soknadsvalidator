import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val junitJupiterVersion = "5.3.1"
val spekVersion = "1.2.1"
val kluentVersion = "1.41"
val fuelVersion = "1.15.1"

val mainClass = "no.nav.helse.AppKt"

plugins {
    kotlin("jvm") version "1.3.11"
}

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.0")
    }
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile("no.nav.helse:streams:16")
    compile("com.github.kittinunf.fuel:fuel:$fuelVersion")

    testCompile("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
    testCompile("org.amshove.kluent:kluent:$kluentVersion")

    testImplementation("no.nav:kafka-embedded-env:2.0.1")

    testCompile("org.skyscreamer:jsonassert:1.2.3")

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

    jcenter()
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.named<KotlinCompile>("compileTestKotlin") {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.named<Jar>("jar") {
    baseName = "app"

    manifest {
        attributes["Main-Class"] = mainClass
        attributes["Class-Path"] = configurations["compile"].map {
            it.name
        }.joinToString(separator = " ")
    }

    configurations["compile"].forEach {
        val file = File("$buildDir/libs/${it.name}")
        if (!file.exists())
            it.copyTo(file)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "5.0"
}
