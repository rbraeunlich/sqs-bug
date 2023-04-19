package dev.code_n_roll.test.sqs

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.messaging.MessageHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted

@SpringBootTest
@Testcontainers
class SenderIntegrationTest(
    @Autowired private val sender: Sender,
    @Autowired private val sqsAsyncClient: SqsAsyncClient,
    @Value("\${some.queueName}") private val queueName: String,
    @Autowired private val objectMapper: ObjectMapper,
) {

    companion object {
        @Container
        val localstack: LocalStackContainer =
            LocalStackContainer(DockerImageName.parse("localstack/localstack:1.4"))
                .withServices(SQS)

        @JvmStatic
        @DynamicPropertySource
        fun awsProperties(registry: DynamicPropertyRegistry) {
            with(registry) {
                add("spring.cloud.aws.region.static", localstack::getRegion)
                add("spring.cloud.aws.credentials.access-key", localstack::getAccessKey)
                add("spring.cloud.aws.credentials.secret-key", localstack::getSecretKey)
                add("spring.cloud.aws.sqs.endpoint") { localstack.getEndpointOverride(SQS) }
            }
        }

        @JvmStatic
        @BeforeAll
        fun setUp() {
            localstack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", "braze-currents-ppids")
        }
    }

    @Test
    fun `should send event`() {
        val event = Event("something")
        sender.send(event)

        await untilAsserted {
            sqsAsyncClient.getQueueUrl { it.queueName(queueName) }
                .thenCompose { urlResponse ->
                    sqsAsyncClient.receiveMessage { it.queueUrl(urlResponse.queueUrl()) }
                }.thenApply { message ->
                    assertThat(
                        message.messages().first().messageAttributes()[MessageHeaders.CONTENT_TYPE]!!.stringValue(),
                    ).isEqualTo(
                        "application/json",
                    )
                    objectMapper.readValue(message.messages().first().body(), Event::class.java)
                }.thenAccept {
                    assertThat(it).isEqualTo(event)
                }
        }
    }
}
