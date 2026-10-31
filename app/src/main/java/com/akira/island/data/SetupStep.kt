package com.akira.island.data

import kotlinx.serialization.Serializable

@Serializable
enum class SetupStep {
    WELCOME, ACCESSIBILITY, NOTIFICATIONS, OPTIONAL, PRIVACY, TRY_IT;

    val previous: SetupStep? get() = entries.getOrNull(ordinal - 1)
    val next: SetupStep? get() = entries.getOrNull(ordinal + 1)
}
