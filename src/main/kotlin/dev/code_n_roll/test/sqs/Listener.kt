package dev.code_n_roll.test.sqs

import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.stereotype.Component

@Component
class Listener {

    @SqsListener(value = ["queueName"])
    fun listen(event: String) {
        println(event)
    }

}