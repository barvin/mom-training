package org.example

import com.rabbitmq.client.Address
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.awaitility.kotlin.atLeast
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.until
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.Duration.ofSeconds
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AppTest {
    private val app = App()

    @BeforeAll
    fun setUp() {
        app.run()
    }

    private fun statusIsSentPeriodicallyData(): List<Arguments> {
        return listOf(
            Arguments.of("durable_status", true, true),
            Arguments.of("transient_status", false, true),
            Arguments.of("durable_status", true, false),
            Arguments.of("transient_status", false, false)
        )
    }

    @ParameterizedTest
    @MethodSource("statusIsSentPeriodicallyData")
    fun statusIsSentPeriodically(queueName: String, durable: Boolean, autoAck: Boolean) {
        QueueListener(queueName, "StatusConsumer", buildConnection(), durable, autoAck)
            .use { listener ->
                await() atMost ofSeconds(7) until { listener.messages.size > 0 }
                await() atLeast ofSeconds(5) atMost ofSeconds(7) until { listener.messages.size > 1 }
                assertThat(listener.messages[0]).contains("[$queueName]")
                assertThat(listener.messages[1]).contains("[$queueName]")
            }
    }

    private fun stringTransformData(): List<Arguments> {
        return listOf(
            Arguments.of("weLcoMe tO tHe real woRLD, neo!", "WELCOME TO THE REAL WORLD, NEO!"),
            Arguments.of("5634 *%^&*#%@)( |><?>\"'''\"d``", "5634 *%^&*#%@)( |><?>\"'''\"D``")
        )
    }

    @ParameterizedTest
    @MethodSource("stringTransformData")
    fun stringTransformationFlow(input: String, output: String) {
        val replyToQueueName = UUID.randomUUID().toString()
        val correlationId = UUID.randomUUID().toString()
        QueueListener(replyToQueueName, "UpperCasedConsumer", buildConnection()).use { listener ->
            listener.correlationId = correlationId

            QueueRpcPublisher("raw_string", buildConnection()).use { publisher ->

                publisher.publish(input, replyToQueueName, correlationId)

                await() atMost ofSeconds(5) until { listener.messages.size > 0 }
                assertThat(listener.messages[0]).isEqualTo(output)
            }
        }
    }

    private fun buildConnection(): Connection {
        val factory = ConnectionFactory()
        factory.username = USER_NAME
        factory.password = System.getenv("RABBIT_PASSWORD")
        factory.virtualHost = USER_NAME
        return factory.newConnection(listOf(Address(RABBIT_MQ_HOST)))
    }
}
