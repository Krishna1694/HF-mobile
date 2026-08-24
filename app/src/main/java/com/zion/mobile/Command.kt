package com.zion.mobile

data class Command(
    val intent: CommandIntent,
    val target: String? = null,
    val parameters: Map<String, String> = emptyMap()
)