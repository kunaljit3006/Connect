package com.example.connect.model

data class User(
    val uid: String = "",         // Firestore document ID (user's UID)
    val name: String = "",        // User's name
    val status: String = "",      // Online/Offline or any status
    val profileUrl: String = "",   // Optional: URL of profile picture
    
    // UI state for friendship status relative to current logged in user
    // "none", "sent", "received", "friends"
    var friendStatus: String = "none",
    
    // Firestore document ID for the friendship record to allow easy tracking
    var friendshipDocId: String? = null,
    
    // Last interaction timestamp (for sorting recent calls)
    var lastInteraction: Long = 0L
)
