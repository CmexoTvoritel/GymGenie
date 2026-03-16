package com.asc.gymgenie

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform