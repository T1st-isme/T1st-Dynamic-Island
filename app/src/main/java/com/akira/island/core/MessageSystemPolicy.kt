package com.akira.island.core

/** Current Android restrictions, separate from the app's user preferences and ingestion snapshot. */
data class MessageSystemPolicy(
    val showOnLock: Boolean = true,
    val showPrivate: Boolean = true,
    val visibilityOverrides: Map<String, MessageVisibility> = emptyMap(),
)

enum class MessageVisibility { PUBLIC, PRIVATE, SECRET }
