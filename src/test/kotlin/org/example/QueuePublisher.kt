package org.example

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import java.io.Closeable

class QueueRpcPublisher(private val queueName: String, private val connection: Connection) : Closeable {
    private val channel: Channel = connection.createChannel()

    init {
        channel.queueDeclare(queueName, true, false, false, null)
    }

    fun publish(message: String, replyTo: String, correlationId: String) {
        val props = AMQP.BasicProperties.Builder()
            .correlationId(correlationId)
            .replyTo(replyTo)
            .build()
        channel.basicPublish("", queueName, props, message.toByteArray())
        println(" [x] Sent '$message'")
    }

    override fun close() {
        channel.close()
        connection.close()
    }
}