package com.example.applicationdemo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.applicationdemo.FirebaseManager
import com.example.applicationdemo.model.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    onBack: () -> Unit,
    filterDepartment: String? = null
) {
    var pendingRequests by remember { mutableStateOf<List<JoinRequest>>(emptyList()) }
    var allRequests by remember { mutableStateOf<List<JoinRequest>>(emptyList()) }
    var analytics by remember { mutableStateOf(AdminAnalytics()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Overview, 1: Pending, 2: Records
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    fun refreshData() {
        scope.launch {
            val pending = FirebaseManager.getPendingJoinRequests()
            val all = FirebaseManager.getAllJoinRequests()
            val stats = FirebaseManager.getAdminAnalytics(filterDepartment)

            pendingRequests = if (filterDepartment != null) {
                pending.filter { it.department.equals(filterDepartment, ignoreCase = true) }
            } else pending

            allRequests = if (filterDepartment != null) {
                all.filter { it.department.equals(filterDepartment, ignoreCase = true) }
            } else all

            analytics = stats
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    val filteredPending = remember(pendingRequests, searchQuery) {
        pendingRequests.filter { 
            it.userName.contains(searchQuery, ignoreCase = true) || 
            it.courseName.contains(searchQuery, ignoreCase = true) 
        }
    }
    
    val filteredAll = remember(allRequests, searchQuery) {
        allRequests.filter { 
            it.userName.contains(searchQuery, ignoreCase = true) || 
            it.courseName.contains(searchQuery, ignoreCase = true) ||
            it.userEmail.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search by name or course...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    } else {
                        Text(
                            text = if (filterDepartment != null) "$filterDepartment Admin Console" else "Central Admin Dashboard",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (isSearchActive) { isSearchActive = false; searchQuery = "" } else onBack() }) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(modifier = Modifier.padding(padding)) {
                SecondaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Overview 📊", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Pending (${filteredPending.size})", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Records (${filteredAll.size})", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                ) {
                    when (selectedTab) {
                        0 -> AdminAnalyticsOverview(
                            analytics = analytics,
                            deptName = filterDepartment,
                            onGoToPending = { selectedTab = 1 },
                            onGoToRecords = { selectedTab = 2 }
                        )
                        1 -> JoinRequestList(filteredPending) { refreshData() }
                        2 -> EnrolledList(filteredAll) { refreshData() }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminAnalyticsOverview(
    analytics: AdminAnalytics,
    deptName: String?,
    onGoToPending: () -> Unit,
    onGoToRecords: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Hero Banner Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(44.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Analytics,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (deptName != null) "$deptName Analytics" else "System Analytics",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text(
                                    text = "Real-time portal performance & records",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                )
                            }
                        }

                        if (analytics.pendingRequests > 0) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                onClick = onGoToPending,
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${analytics.pendingRequests} student access requests waiting for approval!",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Metrics Grid Title
        item {
            Text(
                text = "Key Metrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // 4 Analytics Grid Items (2x2)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    AnalyticsStatCard(
                        title = "Students",
                        value = analytics.totalStudents.toString(),
                        subtitle = "Active Enrolled",
                        icon = Icons.Default.Group,
                        containerColor = MaterialTheme.colorScheme.surface,
                        iconColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = onGoToRecords
                    )
                    AnalyticsStatCard(
                        title = "Pending",
                        value = analytics.pendingRequests.toString(),
                        subtitle = "Requests",
                        icon = Icons.Default.HourglassTop,
                        containerColor = if (analytics.pendingRequests > 0) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
                        iconColor = if (analytics.pendingRequests > 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = onGoToPending
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    AnalyticsStatCard(
                        title = "Shared Notes",
                        value = analytics.totalPosts.toString(),
                        subtitle = "PDFs & Materials",
                        icon = Icons.AutoMirrored.Filled.LibraryBooks,
                        containerColor = MaterialTheme.colorScheme.surface,
                        iconColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    AnalyticsStatCard(
                        title = "Total Access",
                        value = analytics.totalJoinRequests.toString(),
                        subtitle = "All Submissions",
                        icon = Icons.Default.AssignmentInd,
                        containerColor = MaterialTheme.colorScheme.surface,
                        iconColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = onGoToRecords
                    )
                }
            }
        }

        // Quick Actions Section
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onGoToPending() }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(36.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.HowToReg, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Review Access Requests", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text("Approve or decline student join requests", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onGoToRecords() }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(36.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.PeopleAlt, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Manage Enrolled Students", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text("View enrolled users or revoke course access", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .height(130.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = iconColor.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                    }
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun JoinRequestList(requests: List<JoinRequest>, onUpdate: () -> Unit) {
    val scope = rememberCoroutineScope()
    if (requests.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CheckCircleOutline, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(16.dp))
                Text("No pending requests.", color = MaterialTheme.colorScheme.outline)
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(requests) { request ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(request.userName.firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = request.userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = "Course: ${request.courseName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            Text(text = "Dept: ${request.department}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Row {
                            IconButton(onClick = { scope.launch { FirebaseManager.updateJoinRequestStatus(request.id, "DECLINED"); onUpdate() } }, colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                                Icon(Icons.Default.Cancel, contentDescription = null)
                            }
                            IconButton(onClick = { scope.launch { FirebaseManager.updateJoinRequestStatus(request.id, "APPROVED"); onUpdate() } }, colors = IconButtonDefaults.iconButtonColors(contentColor = Color(0xFF4CAF50))) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EnrolledList(allRequests: List<JoinRequest>, onUpdate: () -> Unit) {
    val scope = rememberCoroutineScope()
    val groupedStudents = allRequests.groupBy { it.userId }

    if (groupedStudents.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No records found.", color = MaterialTheme.colorScheme.outline)
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(groupedStudents.keys.toList()) { userId ->
                val studentRequests = groupedStudents[userId] ?: emptyList()
                val firstRequest = studentRequests.first()
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, contentDescription = null) }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = firstRequest.userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(text = firstRequest.userEmail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), thickness = 0.5.dp)
                        studentRequests.forEach { request ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = request.courseName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    StatusBadge(status = request.status)
                                }
                                if (request.status == "APPROVED") {
                                    OutlinedButton(onClick = { scope.launch { FirebaseManager.updateJoinRequestStatus(request.id, "DECLINED"); onUpdate() } }, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Revoke", fontSize = 12.sp) }
                                } else if (request.status == "DECLINED") {
                                    Button(onClick = { scope.launch { FirebaseManager.updateJoinRequestStatus(request.id, "APPROVED"); onUpdate() } }, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text("Restore", fontSize = 12.sp) }
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
fun StatusBadge(status: String) {
    val (color, text) = when (status) {
        "APPROVED" -> Color(0xFF4CAF50) to "Authorized"
        "DECLINED" -> MaterialTheme.colorScheme.error to "Access Revoked"
        else -> MaterialTheme.colorScheme.primary to "Pending"
    }
    Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(top = 4.dp)) {
        Text(text = text, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
    }
}
