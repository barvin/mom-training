package org.example

import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.DeliverCallback
import java.io.Closeable

class QueueListener(
    queueName: String,
    private val consumerTag: String,
    private val connection: Connection,
    durable: Boolean = true,
    autoAck: Boolean = true,
) :
    Closeable {

    val messages = mutableListOf<String>()
    var correlationId: String? = null
    private val channel: Channel = connection.createChannel()

    init {
        channel.queueDeclare(queueName, durable, false, false, null)

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

        channel.basicConsume(queueName, autoAck, consumerTag, deliverCallback, cancelCallback)

        // ignore all the messages that were waiting in the queue before this consumer has joined
        Thread.sleep(1000)
        messages.clear()
    }

    override fun close() {
        channel.basicCancel(consumerTag)
        channel.close()
        connection.close()
    }
}