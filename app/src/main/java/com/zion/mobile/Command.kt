package com.zion.mobile

data class Command(
    val intent: CommandIntent,
    val target: String? = null,
    val recent: Boolean = false,
    val parameters: Map<String, String> = emptyMap()
)