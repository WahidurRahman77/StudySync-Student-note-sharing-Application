package com.example.applicationdemo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.cloudinary.android.MediaManager
import com.example.applicationdemo.model.Notification
import com.example.applicationdemo.navigation.AppNavigation
import com.example.applicationdemo.ui.theme.ApplicationDemoTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MainActivity : ComponentActivity() {
    private var notificationListener: ListenerRegistration? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission result handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermission()

        try {
            val config = hashMapOf(
                "cloud_name" to "aozxe5yv",
                "api_key" to "547215334414622",
                "api_secret" to "6yJQsqmG8FHFexyaA_nigQY-IUM"
            )
            MediaManager.init(this, config)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        enableEdgeToEdge()
        setContent {
            ApplicationDemoTheme {
                AppNavigation()
            }
        }

        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val uid = auth.currentUser?.uid
            if (uid != null) {
                startNotificationListener(uid)
            } else {
                notificationListener?.remove()
                notificationListener = null
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun startNotificationListener(uid: String) {
        notificationListener?.remove()
        var isInitialSnapshot = true

        notificationListener = FirebaseFirestore.getInstance().collection("notifications")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                if (isInitialSnapshot) {
                    isInitialSnapshot = false
                    
                    // Check for unread notifications created while offline / logged out
                    val unreadNotifications = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(Notification::class.java)
                    }?.filter { !it.isRead } ?: emptyList()

                    if (unreadNotifications.isNotEmpty()) {
                        if (unreadNotifications.size == 1) {
                            val notif = unreadNotifications.first()
                            val title = notif.title.ifEmpty { "New Notification" }
                            NotificationHelper.showNotification(this, title, notif.message)
                        } else {
                            NotificationHelper.showNotification(
                                this,
                                "${unreadNotifications.size} New Updates",
                                "You have ${unreadNotifications.size} unread course notifications."
                            )
                        }
                    }
                    return@addSnapshotListener
                }

                // Real-time new additions
                snapshot?.documentChanges?.forEach { dc ->
                    if (dc.type == DocumentChange.Type.ADDED) {
                        val notification = dc.document.toObject(Notification::class.java)
                        if (!notification.isRead) {
                            val title = notification.title.ifEmpty { "New Notification" }
                            val message = notification.message
                            NotificationHelper.showNotification(this, title, message)
                        }
                    }
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationListener?.remove()
    }
}
