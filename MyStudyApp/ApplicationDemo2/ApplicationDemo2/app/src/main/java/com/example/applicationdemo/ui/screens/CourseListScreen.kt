package com.example.applicationdemo.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.applicationdemo.FirebaseManager
import com.example.applicationdemo.model.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseListScreen(
    department: String,
    onNavigateToFeed: (String, String) -> Unit,
    onNavigateToAdmin: () -> Unit, // New callback
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var courses by remember { mutableStateOf<List<Course>>(emptyList()) }
    var joinRequests by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var currentUser by remember { mutableStateOf<User?>(null) }
    
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var showAddCourseDialog by remember { mutableStateOf(false) }

    LaunchedEffect(department) {
        val uid = FirebaseManager.currentUser?.uid
        if (uid != null) {
            currentUser = FirebaseManager.getUserData(uid)
            FirebaseManager.observeUserJoinRequests(uid) { joinRequests = it }
        }
        
        if (department == "CSE") { FirebaseManager.forceSeedCseCourses() }
        courses = FirebaseManager.getCoursesByDepartment(department)
        isLoading = false
    }

    val filteredCourses = remember(courses, searchQuery) {
        courses.filter { it.name.contains(searchQuery, ignoreCase = true) || it.code.contains(searchQuery, ignoreCase = true) }
    }

    if (showAddCourseDialog) {
        var courseName by remember { mutableStateOf("") }
        var courseCode by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCourseDialog = false },
            title = { Text("Add Course to $department") },
            text = {
                Column {
                    OutlinedTextField(value = courseName, onValueChange = { courseName = it }, label = { Text("Course Name") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = courseCode, onValueChange = { courseCode = it }, label = { Text("Course Code") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (courseName.isNotBlank() && courseCode.isNotBlank()) {
                        scope.launch {
                            FirebaseManager.createCourse(Course(name = courseName, code = courseCode, department = department))
                            courses = FirebaseManager.getCoursesByDepartment(department)
                            showAddCourseDialog = false
                        }
                    }
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddCourseDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    if (isSearchActive) {
                        TextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("Search course...") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
                    } else { Text("$department Department") }
                },
                navigationIcon = {
                    IconButton(onClick = { if (isSearchActive) { isSearchActive = false; searchQuery = "" } else onBack() }) {
                        Icon(imageVector = if (isSearchActive) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        IconButton(onClick = { isSearchActive = true }) { Icon(Icons.Default.Search, contentDescription = null) }
                        if (currentUser?.role == "ADMIN") {
                            IconButton(onClick = onNavigateToAdmin) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = "Manage $department")
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentUser?.role == "ADMIN") {
                FloatingActionButton(onClick = { showAddCourseDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Course")
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))) {
                Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
                    Text(text = "Available Courses", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(filteredCourses) { course ->
                        val status = if (currentUser?.role == "ADMIN") "APPROVED" else joinRequests[course.id] ?: "NONE"
                        CourseCard(
                            course = course,
                            status = status,
                            onJoin = {
                                scope.launch {
                                    FirebaseManager.createJoinRequest(JoinRequest(userId = currentUser?.uid ?: "", userName = currentUser?.name ?: "User", userEmail = currentUser?.email ?: "", courseId = course.id, courseName = course.name, department = department, status = "PENDING"))
                                    Toast.makeText(context, "Request submitted", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onEnter = { onNavigateToFeed(course.id, course.name) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CourseCard(course: Course, status: String, onJoin: () -> Unit, onEnter: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(50.dp), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Book, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = course.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "Course Code: ${course.code}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
            }
            when (status) {
                "APPROVED" -> { IconButton(onClick = onEnter) { Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, tint = MaterialTheme.colorScheme.primary) } }
                "PENDING" -> { Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) { Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline); Spacer(modifier = Modifier.width(4.dp)); Text("Pending", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline) } } }
                else -> { OutlinedButton(onClick = onJoin, shape = RoundedCornerShape(12.dp)) { Text("Join", fontSize = 12.sp) } }
            }
        }
    }
}
