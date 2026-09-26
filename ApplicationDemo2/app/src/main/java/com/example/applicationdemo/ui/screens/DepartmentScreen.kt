package com.example.applicationdemo.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.applicationdemo.FirebaseManager
import com.example.applicationdemo.R
import com.example.applicationdemo.model.Department
import com.example.applicationdemo.model.User
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartmentScreen(
    onNavigateToCourses: (String) -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToMyNotes: () -> Unit,
    onLogout: () -> Unit
) {
    var departments by remember { mutableStateOf<List<Department>>(emptyList()) }
    var currentUser by remember { mutableStateOf<User?>(null) }
    var unreadCount by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val uid = FirebaseManager.currentUser?.uid
        if (uid != null) {
            currentUser = FirebaseManager.getUserData(uid)
            FirebaseManager.observeUnreadCount(uid) { count ->
                unreadCount = count
            }
        }
        departments = FirebaseManager.getAllDepartments()
        if (departments.isEmpty()) {
            val initial = listOf("CSE", "EEE", "BBA", "English", "Law")
            initial.forEach { FirebaseManager.addDepartment(it) }
            departments = FirebaseManager.getAllDepartments()
        }
        isLoading = false
    }

    val filteredDepartments = remember(departments, searchQuery) {
        departments
            .distinctBy { it.name.trim().uppercase() }
            .filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    if (showAddDialog) {
        var newDeptName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add New Department", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newDeptName,
                    onValueChange = { newDeptName = it },
                    label = { Text("Department Name (e.g. CSE)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newDeptName.isNotBlank()) {
                        scope.launch {
                            FirebaseManager.addDepartment(newDeptName)
                            departments = FirebaseManager.getAllDepartments()
                            showAddDialog = false
                        }
                    }
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search departments...") },
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_app_icon),
                                contentDescription = "Logo",
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("StudySync", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                    }
                },
                navigationIcon = {
                    if (isSearchActive) {
                        IconButton(onClick = { 
                            isSearchActive = false 
                            searchQuery = ""
                        }) { Icon(Icons.Default.Close, contentDescription = null) }
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        IconButton(onClick = { isSearchActive = true }) { 
                            Icon(Icons.Default.Search, contentDescription = "Search") 
                        }
                        
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge { Text(unreadCount.toString()) }
                                }
                            }
                        ) {
                            IconButton(onClick = onNavigateToNotifications) {
                                Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                            }
                        }

                        IconButton(onClick = onNavigateToProfile) { 
                            Icon(Icons.Default.AccountCircle, contentDescription = "Profile") 
                        }

                        IconButton(onClick = onLogout) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (currentUser?.role == "ADMIN") {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add Dept") },
                    containerColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            ) {
                // Header Banner & Shortcuts (Full Width)
                item(span = { GridItemSpan(2) }) {
                    Column {
                        // Premium Banner Card
                        StudentWelcomeBanner(
                            user = currentUser,
                            onNavigateToProfile = onNavigateToProfile,
                            onNavigateToMyNotes = onNavigateToMyNotes
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Quick Shortcuts Row
                        QuickShortcutsBar(
                            unreadCount = unreadCount,
                            isAdmin = currentUser?.role == "ADMIN",
                            onMyNotes = onNavigateToMyNotes,
                            onNotifications = onNavigateToNotifications,
                            onProfile = onNavigateToProfile,
                            onAdmin = onNavigateToAdmin
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Section Title
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Academic Faculties",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${filteredDepartments.size} Depts",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                // Department Cards (2 Column Grid)
                items(filteredDepartments) { dept ->
                    PremiumDepartmentCard(dept.name) { onNavigateToCourses(dept.name) }
                }
            }
        }
    }
}

@Composable
fun StudentWelcomeBanner(
    user: User?,
    onNavigateToProfile: () -> Unit,
    onNavigateToMyNotes: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Profile Image / Initials
                    Surface(
                        modifier = Modifier
                            .size(56.dp)
                            .clickable { onNavigateToProfile() },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (user?.profileImage?.isNotEmpty() == true) {
                                AsyncImage(
                                    model = user.profileImage,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = user?.name?.firstOrNull()?.uppercase() ?: "S",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hello, ${user?.name ?: "Student"}! 👋",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (user?.studentId?.isNotEmpty() == true) "ID: ${user.studentId} • Premier University" else "Premier University • Study Portal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Banner Footer
                Surface(
                    onClick = onNavigateToMyNotes,
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Quick Access: Saved Notes & PDFs",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickShortcutsBar(
    unreadCount: Int,
    isAdmin: Boolean,
    onMyNotes: () -> Unit,
    onNotifications: () -> Unit,
    onProfile: () -> Unit,
    onAdmin: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ShortcutItem(
            icon = Icons.Default.Bookmark,
            label = "Saved Notes",
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f),
            onClick = onMyNotes
        )
        ShortcutItem(
            icon = Icons.Default.Notifications,
            label = if (unreadCount > 0) "Alerts ($unreadCount)" else "Alerts",
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f),
            onClick = onNotifications
        )
        if (isAdmin) {
            ShortcutItem(
                icon = Icons.Default.Dashboard,
                label = "Admin",
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1f),
                onClick = onAdmin
            )
        } else {
            ShortcutItem(
                icon = Icons.Default.Person,
                label = "Profile",
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                onClick = onProfile
            )
        }
    }
}

@Composable
fun ShortcutItem(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(72.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PremiumDepartmentCard(name: String, onClick: () -> Unit) {
    val (icon, subtitle) = when (name.uppercase()) {
        "CSE" -> Icons.Default.Computer to "Computer Science"
        "EEE" -> Icons.Default.ElectricBolt to "Electrical & Electronic"
        "BBA" -> Icons.Default.Business to "Business Admin"
        "ENGLISH" -> Icons.AutoMirrored.Filled.MenuBook to "Dept of English"
        "LAW" -> Icons.Default.Gavel to "School of Law"
        else -> Icons.Default.School to "Academic Department"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
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
