![CI](https://github.com/barvin/mom-training/workflows/CI/badge.svg?branch=main)
# MOM training
Playing with RabbitMQ (Message-Oriented Middleware) and its testing

## Application
The task was done in the following way:
1. https://www.cloudamqp.com/ was used for hosting RabbitMQ server
2. All the implementation was done in one class: [App.kt](src/main/kotlin/org/example/App.kt)
   - Messages containing current time in millis are sent every 3 seconds to one of 2 queues (durable and transient). Each time it goes to a different queue, so each queue gets message every 6 seconds. See `pushStatusPeriodically` method.
   - Request/reply handler transforms the incoming message to upper case and replies to the `reply_to` queue. Artificial sleep for 3 seconds was added to emulate the computation. See `listenAndReplyWithUpperCaseMessage` method.

## Tests
- Tests are implemented in the same project, but they are absolutely independent of the application itself, just done in the same repo for convenience. 
- All tests are implemented in [AppTest.kt](src/test/kotlin/org/example/AppTest.kt)
- Added classes [QueueListener](src/test/kotlin/org/example/QueueListener.kt) that encapsulates consumer logic and [QueuePublisher](src/test/kotlin/org/example/QueuePublisher.kt) that encapsulates producer logic
- Tests for eternal producer are done as a parametrized test method, where we check all combinations of durable/transient queue and sending/not sending automatic acknowledgement:
  ```kotlin
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
  ```
- Tests for request/reply handler are done as parametrized test method as well, where I just try different input messages. But that is just an example, for the real system I would come up with more cases covering different alphabets, all edge cases.
  ```kotlin
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
  ```