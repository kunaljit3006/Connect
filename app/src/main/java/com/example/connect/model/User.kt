package com.example.connect.model

data class User(
    val name: String = "",        // User's name
    val status: String = "",      // Online/Offline or any status
    val profileUrl: String = ""   // Optional: URL of profile picture
)
