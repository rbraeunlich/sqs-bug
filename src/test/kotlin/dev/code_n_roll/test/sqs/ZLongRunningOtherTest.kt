package dev.code_n_roll.test.sqs

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ZLongRunningOtherTest {

    @Test
    fun `should take long`() {
        repeat(1000) {
            Thread.sleep(100L)
            assertThat(true).isTrue()
        }
    }
}