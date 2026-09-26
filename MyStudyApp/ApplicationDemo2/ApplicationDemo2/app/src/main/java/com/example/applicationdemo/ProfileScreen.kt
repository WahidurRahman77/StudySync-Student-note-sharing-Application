package com.example.applicationdemo

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.applicationdemo.CloudinaryHelper
import com.example.applicationdemo.FirebaseManager
import com.example.applicationdemo.model.User
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onProfileSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var user by remember { mutableStateOf<User?>(null) }
    var isEditMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    // Form states
    var fullName by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var activeCourses by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var existingImageUrl by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    LaunchedEffect(userId) {
        val fetchedUser = FirebaseManager.getUserData(userId)
        if (fetchedUser != null) {
            user = fetchedUser
            fullName = fetchedUser.name
            studentId = fetchedUser.studentId
            department = fetchedUser.department
            activeCourses = fetchedUser.activeCourses
            existingImageUrl = fetchedUser.profileImage
            isEditMode = false
        } else {
            isEditMode = true
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Configure Profile" else "Account Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isEditMode && user != null) {
                        TextButton(onClick = { isEditMode = true }) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                // Background Gradient Decor
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), Color.Transparent)
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(20.dp))

                    // Profile Image Section
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Surface(
                            modifier = Modifier
                                .size(140.dp)
                                .padding(4.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 4.dp,
                            border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedImageUri != null) {
                                    AsyncImage(
                                        model = selectedImageUri,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else if (existingImageUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = existingImageUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(80.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }

                        if (isEditMode) {
                            SmallFloatingActionButton(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                shape = CircleShape,
                                modifier = Modifier.offset(x = (-8).dp, y = (-8).dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Change Photo", modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (isEditMode) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = { Text("Display Name") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = studentId,
                                onValueChange = { if (it.all { char -> char.isDigit() }) studentId = it },
                                label = { Text("Student ID") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = department,
                                onValueChange = { department = it },
                                label = { Text("Department") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = { Icon(Icons.Default.AccountTree, contentDescription = null) }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = activeCourses,
                                onValueChange = { activeCourses = it },
                                label = { Text("Courses (comma separated)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = { Icon(Icons.Default.Book, contentDescription = null) }
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            if (isSaving) {
                                CircularProgressIndicator()
                            } else {
                                Button(
                                    onClick = {
                                        if (fullName.isNotBlank() && studentId.isNotBlank()) {
                                            isSaving = true
                                            scope.launch {
                                                var finalImageUrl = existingImageUrl
                                                if (selectedImageUri != null) {
                                                    val imageUrl = CloudinaryHelper.uploadFile(context, selectedImageUri!!)
                                                    if (imageUrl != null) {
                                                        finalImageUrl = imageUrl
                                                        saveUserToFirestore(userId, fullName, studentId, department, activeCourses, finalImageUrl) {
                                                            isSaving = false
                                                            isEditMode = false
                                                            Toast.makeText(context, "Profile Updated!", Toast.LENGTH_SHORT).show()
                                                            onProfileSaved()
                                                        }
                                                    } else {
                                                        isSaving = false
                                                        Toast.makeText(context, "Upload Failed", Toast.LENGTH_SHORT).show()
                                                    }
                                                } else {
                                                    saveUserToFirestore(userId, fullName, studentId, department, activeCourses, finalImageUrl) {
                                                        isSaving = false
                                                        isEditMode = false
                                                        Toast.makeText(context, "Profile Updated!", Toast.LENGTH_SHORT).show()
                                                        onProfileSaved()
                                                    }
                                                }
                                            }
                                        } else {
                                            Toast.makeText(context, "Please fill required fields", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Complete Update", fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            TextButton(onClick = { isEditMode = false }, modifier = Modifier.padding(top = 8.dp)) {
                                Text("Cancel Changes", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    } else {
                        // View Mode
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                        ) {
                            Text(
                                text = fullName,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            Text(
                                text = user?.email ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )

                            Spacer(modifier = Modifier.height(32.dp))
                            
                            ProfileCardInfo(label = "Student ID", value = studentId, icon = Icons.Default.Badge)
                            ProfileCardInfo(label = "Academic Institution", value = "Premier University", icon = Icons.Default.School)
                            ProfileCardInfo(label = "Primary Department", value = department, icon = Icons.Default.AccountTree)
                            ProfileCardInfo(label = "Enrolled Courses", value = activeCourses, icon = Icons.AutoMirrored.Filled.LibraryBooks)
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Account Status", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Your account is ${user?.approvedStatus?.lowercase() ?: "active"}. You have ${if (user?.role == "ADMIN") "full administrative" else "student"} access.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
fun ProfileCardInfo(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Text(text = if (value.isEmpty()) "Not set" else value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun saveUserToFirestore(
    uid: String,
    name: String,
    studentId: String,
    dept: String,
    courses: String,
    imageUrl: String,
    onComplete: () -> Unit
) {
    val updatedUser = mapOf(
        "name" to name,
        "studentId" to studentId,
        "department" to dept,
        "activeCourses" to courses,
        "profileImage" to imageUrl,
        "university" to "Premier University"
    )
    
    com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users")
        .document(uid)
        .set(updatedUser, com.google.firebase.firestore.SetOptions.merge())
        .addOnCompleteListener { onComplete() }
}
