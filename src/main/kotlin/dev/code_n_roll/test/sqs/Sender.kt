package dev.code_n_roll.test.sqs

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Lazy
import org.springframework.messaging.MessageHeaders
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue

@Service
class Sender(
    @Lazy private val sqsAsyncClient: SqsAsyncClient,
    @Value("\${some.queueName}") private val queueName: String,
    @Autowired private val objectMapper: ObjectMapper,
) {

    fun send(event: Event) {
        sqsAsyncClient.convertAndSend(queueName, event, objectMapper)
    }
}

private fun SqsAsyncClient.convertAndSend(queueName: String, event: Event, objectMapper: ObjectMapper) {
    this
        .getQueueUrl { it.queueName(queueName) }
        .thenAccept { urlResponse ->
            this.sendMessage {
                it.queueUrl(urlResponse.queueUrl())
                    .messageAttributes(
                        mapOf(
                            MessageHeaders.CONTENT_TYPE to MessageAttributeValue.builder()
                                .stringValue("application/json").build(),
                        ),
                    )
                    .messageBody(objectMapper.writeValueAsString(event))
            }
        }
}

data class Event(val content: String)