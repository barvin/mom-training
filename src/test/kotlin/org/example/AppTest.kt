package org.example

import com.rabbitmq.client.Address
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.until
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
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

    @Test
    fun statusIsSentPeriodically() {
        val listener = QueueListener("status", "StatusConsumer", buildConnection())
        listener.messages.clear()
        await() atMost ofSeconds(6) until { listener.messages.size > 0 }
        await() atMost ofSeconds(6) until { listener.messages.size > 1 }
        assertThat(listener.messages[0]).isNotEqualTo(listener.messages[1])
        listener.close()
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
        val replyToQueueName = "upper_cased"
        val correlationId = UUID.randomUUID().toString()
        val listener = QueueListener(replyToQueueName, "UpperCasedConsumer", buildConnection())
        listener.messages.clear()
        listener.correlationId = correlationId

        val publisher = QueueRpcPublisher("raw_string", buildConnection())
        publisher.publish(input, replyToQueueName, correlationId)

        await() atMost ofSeconds(10) until { listener.messages.size > 0 }
        assertThat(listener.messages[0]).isEqualTo(output)

        listener.close()
        publisher.close()
    }

    private fun buildConnection(): Connection {
        val factory = ConnectionFactory()
        factory.username = USER_NAME
        factory.password = System.getenv("RABBIT_PASSWORD")
        factory.virtualHost = USER_NAME
        return factory.newConnection(listOf(Address(RABBIT_MQ_HOST)))
    }
}
