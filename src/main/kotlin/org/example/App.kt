package org.example

import com.rabbitmq.client.*
import java.nio.charset.StandardCharsets
import kotlin.concurrent.thread

const val RABBIT_MQ_HOST = "cow.rmq2.cloudamqp.com"
const val USER_NAME = "zfxtxlyp"
const val STATUS_DELAY_IN_MILLIS: Long = 3000
const val COMPUTATION_TIME_IN_MILLIS: Long = 3000

class App {

    fun run() {
        thread { pushStatusPeriodically() }
        thread { listenAndReplyWithUpperCaseMessage() }
    }

    private fun pushStatusPeriodically() {
        buildConnection().use { connection ->
            connection.createChannel().use { channel ->
                val durableQueueName = "durable_status"
                val transientQueueName = "transient_status"
                channel.queueDeclare(durableQueueName, true, false, false, null)
                channel.queueDeclare(transientQueueName, false, false, false, null)
                var queueName = durableQueueName
                while (true) {
                    Thread.sleep(STATUS_DELAY_IN_MILLIS)
                    val message = "[$queueName] Current time is ${System.currentTimeMillis()}"
                    channel.basicPublish(
                        "", queueName, null, message.toByteArray(StandardCharsets.UTF_8)
                    )
                    println("[StatusProducer] Sent '$message'")
                    queueName = if (queueName == durableQueueName) transientQueueName else durableQueueName
                }
            }
        }
    }

    private fun listenAndReplyWithUpperCaseMessage() {
        val channel = buildConnection().createChannel()
        val queueName = "raw_string"
        val consumerTag = "StringTransformerConsumer"

        channel.queueDeclare(queueName, true, false, false, null)

        println("[$consumerTag] Waiting for messages...")
        val deliverCallback = DeliverCallback { tag, delivery ->
            val replyProps = AMQP.BasicProperties.Builder()
                .correlationId(delivery.properties.correlationId)
                .build()
            val message = String(delivery.body, charset("UTF-8"))
            println("[$tag] Received message: '$message'")
            val response = message.uppercase()
            Thread.sleep(COMPUTATION_TIME_IN_MILLIS)
            channel.basicPublish("", delivery.properties.replyTo, replyProps, response.toByteArray())
            println("[$tag] Responded with: '$response'")

        }
        val cancelCallback = CancelCallback { tag ->
            println("[$tag] was canceled")
        }
        val shutdownSignalCallback = ConsumerShutdownSignalCallback { tag, exception ->
            println("[$tag] was shut down")
            exception.printStackTrace()
        }

        channel.basicConsume(queueName, true, consumerTag, deliverCallback, cancelCallback, shutdownSignalCallback)
    }

    private fun buildConnection(): Connection {
        val factory = ConnectionFactory()
        factory.username = USER_NAME
        factory.password = System.getenv("RABBIT_PASSWORD")
        factory.virtualHost = USER_NAME
        return factory.newConnection(listOf(Address(RABBIT_MQ_HOST)))
    }
}

fun main() {
    App().run()
}
