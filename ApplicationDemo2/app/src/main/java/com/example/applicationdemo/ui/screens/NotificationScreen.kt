package com.example.applicationdemo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.applicationdemo.FirebaseManager
import com.example.applicationdemo.model.Notification
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class NotificationFilter {
    ALL, UNREAD, READ
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onBack: () -> Unit,
    onNavigateToCourse: (String, String) -> Unit
) {
    var notifications by remember { mutableStateOf<List<Notification>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf(NotificationFilter.ALL) }
    
    val userId = FirebaseManager.currentUser?.uid ?: ""
    val scope = rememberCoroutineScope()

    LaunchedEffect(userId) {
        FirebaseManager.observeNotifications(userId) { updatedList ->
            val distinctList = updatedList.distinctBy { 
                if (it.id.isNotEmpty()) it.id else "${it.title}_${it.message}_${it.timestamp}" 
            }
            notifications = distinctList
            isLoading = false
        }
    }

    val unreadCount = remember(notifications) { notifications.count { !it.isRead } }

    val filteredNotifications = remember(notifications, selectedFilter) {
        when (selectedFilter) {
            NotificationFilter.ALL -> notifications
            NotificationFilter.UNREAD -> notifications.filter { !it.isRead }
            NotificationFilter.READ -> notifications.filter { it.isRead }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Notifications", fontWeight = FontWeight.Bold)
                        if (unreadCount > 0) {
                            Text("$unreadCount unread", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (unreadCount > 0) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    FirebaseManager.markAllAsRead(userId)
                                }
                            }
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mark all read", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == NotificationFilter.ALL,
                    onClick = { selectedFilter = NotificationFilter.ALL },
                    label = { Text("All (${notifications.size})") }
                )
                FilterChip(
                    selected = selectedFilter == NotificationFilter.UNREAD,
                    onClick = { selectedFilter = NotificationFilter.UNREAD },
                    label = { Text("Unread ($unreadCount)") }
                )
                FilterChip(
                    selected = selectedFilter == NotificationFilter.READ,
                    onClick = { selectedFilter = NotificationFilter.READ },
                    label = { Text("Read (${notifications.size - unreadCount})") }
                )
            }

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredNotifications.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.NotificationsNone, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = when (selectedFilter) {
                                NotificationFilter.UNREAD -> "No unread notifications"
                                NotificationFilter.READ -> "No read notifications"
                                else -> "No notifications yet"
                            },
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredNotifications, key = { if (it.id.isNotEmpty()) it.id else "${it.timestamp}_${it.title}" }) { notification ->
                        NotificationItem(notification) {
                            scope.launch {
                                if (!notification.isRead && notification.id.isNotEmpty()) {
                                    FirebaseManager.markNotificationAsRead(notification.id)
                                }
                                if (notification.courseId.isNotEmpty() && notification.courseName.isNotEmpty()) {
                                    onNavigateToCourse(notification.courseId, notification.courseName)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(notification: Notification, onClick: () -> Unit) {
    val dateStr = remember(notification.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
        sdf.format(Date(notification.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            if (!notification.isRead) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = if (!notification.isRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Notifications, 
                        contentDescription = null, 
                        modifier = Modifier.size(20.dp), 
                        tint = if (!notification.isRead) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title, 
                    style = MaterialTheme.typography.titleSmall, 
                    fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = notification.message, 
                    style = MaterialTheme.typography.bodyMedium, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateStr, 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.outline
                )
            }
            if (!notification.isRead) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    modifier = Modifier.size(10.dp), 
                    shape = CircleShape, 
                    color = MaterialTheme.colorScheme.primary
                ) {}
            }
        }
    }
}
