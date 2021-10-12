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
import java.time.Duration.ofSeconds

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

    private fun buildConnection(): Connection {
        val factory = ConnectionFactory()
        factory.username = USER_NAME
        factory.password = System.getenv("RABBIT_PASSWORD")
        factory.virtualHost = USER_NAME
        return factory.newConnection(listOf(Address(RABBIT_MQ_HOST)))
    }
}
