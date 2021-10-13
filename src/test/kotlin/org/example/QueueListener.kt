package org.example

import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.DeliverCallback

class QueueListener(queueName: String, private val consumerTag: String, private val connection: Connection) {
    val messages = mutableListOf<String>()
    var correlationId: String? = null
    private val channel: Channel = connection.createChannel()

    init {
        channel.queueDeclare(queueName, true, false, false, null)

        println("[$consumerTag] Waiting for messages...")

        val deliverCallback = DeliverCallback { tag, delivery ->
            if (correlationId == null || delivery.properties.correlationId == correlationId) {
                val message = String(delivery.body, charset("UTF-8"))
                println("[$tag] Received message: '$message'")
                messages.add(message)
            }
        }
        val cancelCallback = CancelCallback { tag ->
            println("[$tag] was canceled")
        }

        channel.basicConsume(queueName, true, consumerTag, deliverCallback, cancelCallback)
    }

    fun close() {
        channel.basicCancel(consumerTag)
        channel.close()
        connection.close()
    }
}