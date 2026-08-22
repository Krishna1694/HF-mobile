package com.zion.mobile

data class Command(
    val intent: CommandIntent,
    val text: String
)