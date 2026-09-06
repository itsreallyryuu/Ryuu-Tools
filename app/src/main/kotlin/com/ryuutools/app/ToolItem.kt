package com.ryuutools.app

data class ToolItem(
    val title: String,
    val iconRes: Int,
    val isReady: Boolean // true = fitur sudah jadi, false = "coming soon"
)