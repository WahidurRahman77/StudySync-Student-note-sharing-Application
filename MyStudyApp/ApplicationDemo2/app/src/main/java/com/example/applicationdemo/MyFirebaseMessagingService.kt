package com.example.applicationdemo

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        // This is triggered for actual FCM push notifications from a server
        remoteMessage.notification?.let {
            NotificationHelper.showNotification(this, it.title ?: "New Update", it.body ?: "")
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // You would typically save this token to Firestore to send targetted notifications
    }
}
