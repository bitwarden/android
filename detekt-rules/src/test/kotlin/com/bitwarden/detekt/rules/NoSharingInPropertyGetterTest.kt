package com.bitwarden.detekt.rules

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NoSharingInPropertyGetterTest {
    private val rule = NoSharingInPropertyGetter(Config.empty)

    @Test
    fun `expression body getter calling stateIn should be reported`() {
        val findings = rule.lint(
            """
            class Repo(private val source: Flow<Int>, private val scope: CoroutineScope) {
                val flow: StateFlow<Int>
                    get() = source.stateIn(scope, SharingStarted.Eagerly, 0)
            }
            """.trimIndent(),
        )

        assertEquals(1, findings.size)
    }

    @Test
    fun `expression body getter calling shareIn should be reported`() {
        val findings = rule.lint(
            """
            class Repo(private val source: Flow<Int>, private val scope: CoroutineScope) {
                val flow: SharedFlow<Int>
                    get() = source.shareIn(scope, SharingStarted.Eagerly)
            }
            """.trimIndent(),
        )

        assertEquals(1, findings.size)
    }

    @Test
    fun `block body getter calling stateIn should be reported`() {
        val findings = rule.lint(
            """
            class Repo(private val source: Flow<Int>, private val scope: CoroutineScope) {
                val flow: StateFlow<Int>
                    get() {
                        return source.map { it + 1 }.stateIn(scope, SharingStarted.Eagerly, 0)
                    }
            }
            """.trimIndent(),
        )

        assertEquals(1, findings.size)
    }

    @Test
    fun `initialized property calling stateIn should not be reported`() {
        val findings = rule.lint(
            """
            class Repo(private val source: Flow<Int>, private val scope: CoroutineScope) {
                val flow: StateFlow<Int> = source.stateIn(scope, SharingStarted.Eagerly, 0)
            }
            """.trimIndent(),
        )

        assertEquals(0, findings.size)
    }

    @Test
    fun `getter returning a cold flow should not be reported`() {
        val findings = rule.lint(
            """
            class Repo(private val source: Flow<Int>) {
                val flow: Flow<Int>
                    get() = source.map { it + 1 }.onStart { emit(0) }
            }
            """.trimIndent(),
        )

        assertEquals(0, findings.size)
    }

    @Test
    fun `getter returning an existing state flow should not be reported`() {
        val findings = rule.lint(
            """
            class Repo {
                private val mutableFlow = MutableStateFlow(0)
                val flow: StateFlow<Int>
                    get() = mutableFlow
            }
            """.trimIndent(),
        )

        assertEquals(0, findings.size)
    }

    @Test
    fun `stateIn outside of a getter should not be reported`() {
        val findings = rule.lint(
            """
            class Repo(private val source: Flow<Int>, private val scope: CoroutineScope) {
                fun createFlow(): StateFlow<Int> =
                    source.stateIn(scope, SharingStarted.Eagerly, 0)
            }
            """.trimIndent(),
        )

        assertEquals(0, findings.size)
    }
}
