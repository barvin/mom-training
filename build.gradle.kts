plugins {
    kotlin("jvm") version "1.5.31"
    application
}

version = "0.1.0"
group = "org.example"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.rabbitmq:amqp-client:5.13.1")
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.11")
    implementation("org.slf4j:slf4j-api:1.7.32")
    implementation("ch.qos.logback:logback-classic:1.2.6")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
    testImplementation("org.assertj:assertj-core:3.21.0")
    testImplementation("org.awaitility:awaitility-kotlin:4.1.0")
}

application {
    mainClass.set("org.example.ProducerApp")
}

tasks.test {
    dependsOn("cleanTest")
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}