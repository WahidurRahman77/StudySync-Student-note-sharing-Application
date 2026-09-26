package com.example.applicationdemo.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.applicationdemo.FirebaseManager
import com.example.applicationdemo.R
import com.example.applicationdemo.model.User
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var showPasskeyDialog by remember { mutableStateOf(false) }
    var enteredPasskey by remember { mutableStateOf("") }

    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var isSendingReset by remember { mutableStateOf(false) }

    // Admin Verification Dialog
    if (showPasskeyDialog) {
        AlertDialog(
            onDismissRequest = { 
                showPasskeyDialog = false
                FirebaseAuth.getInstance().signOut()
            },
            title = { Text("Admin Verification", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column {
                    Text("Please enter the 4-digit admin passkey to continue.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = enteredPasskey,
                        onValueChange = { if (it.length <= 4) enteredPasskey = it },
                        label = { Text("Passkey") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (enteredPasskey == "4444") {
                        showPasskeyDialog = false
                        onLoginSuccess()
                    } else {
                        Toast.makeText(context, "Incorrect Passkey", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Verify")
                }
            }
        )
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSendingReset) showForgotPasswordDialog = false },
            title = { Text("Reset Password", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter your registered email address to receive a password reset link.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resetEmail.isBlank()) {
                            Toast.makeText(context, "Please enter your email", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSendingReset = true
                        FirebaseAuth.getInstance().sendPasswordResetEmail(resetEmail.trim())
                            .addOnCompleteListener { task ->
                                isSendingReset = false
                                if (task.isSuccessful) {
                                    Toast.makeText(context, "Password reset link sent to $resetEmail", Toast.LENGTH_LONG).show()
                                    showForgotPasswordDialog = false
                                    resetEmail = ""
                                } else {
                                    Toast.makeText(context, "Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                    },
                    enabled = !isSendingReset
                ) {
                    if (isSendingReset) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Send Link")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showForgotPasswordDialog = false },
                    enabled = !isSendingReset
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_app_icon),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "StudySync",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Premier University Academic Portal",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(40.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        resetEmail = email.trim()
                        showForgotPasswordDialog = true
                    }
                ) {
                    Text("Forgot Password?", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isLoading = true
                        FirebaseAuth.getInstance().signInWithEmailAndPassword(email.trim(), password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val userUid = task.result?.user?.uid ?: ""
                                    scope.launch {
                                        val userData = FirebaseManager.getUserData(userUid)
                                        isLoading = false
                                        if (userData?.role == "ADMIN") {
                                            showPasskeyDialog = true
                                        } else if (userData?.approvedStatus == "BANNED") {
                                            FirebaseAuth.getInstance().signOut()
                                            Toast.makeText(context, "Your account is banned.", Toast.LENGTH_LONG).show()
                                        } else {
                                            onLoginSuccess()
                                        }
                                    }
                                } else {
                                    isLoading = false
                                    Toast.makeText(context, "Login Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Sign In", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onNavigateToRegister) {
                Text("New here? Create an account", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isAdmin by remember { mutableStateOf(false) }
    var passkey by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Account") },
                navigationIcon = {
                    IconButton(onClick = onNavigateToLogin) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Join Premier University",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "StudySync Academic Community",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = studentId,
                onValueChange = { if (it.all { char -> char.isDigit() }) studentId = it },
                label = { Text("Student ID (Mandatory)") },
                placeholder = { Text("e.g. 0222410005101004") },
                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = isAdmin, onCheckedChange = { isAdmin = it })
                    Text("Register as Instructor/Admin", style = MaterialTheme.typography.labelLarge)
                }
            }

            if (isAdmin) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = passkey,
                    onValueChange = { if (it.length <= 4) passkey = it },
                    label = { Text("Admin Verification Passkey") },
                    leadingIcon = { Icon(Icons.Default.VerifiedUser, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        val cleanName = name.trim()
                        val cleanEmail = email.trim()
                        val cleanStudentId = studentId.trim()
                        
                        if (cleanName.isBlank() || cleanEmail.isBlank() || password.isBlank()) {
                            Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (cleanStudentId.isBlank()) {
                            Toast.makeText(context, "Student ID is mandatory!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (cleanStudentId.length < 8) {
                            Toast.makeText(context, "Please enter a valid Student ID", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (isAdmin && passkey != "4444") {
                            Toast.makeText(context, "Invalid Admin Passkey", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isLoading = true

                        scope.launch {
                            // Check Student ID uniqueness
                            val isAlreadyRegistered = FirebaseManager.isStudentIdRegistered(cleanStudentId)
                            if (isAlreadyRegistered) {
                                isLoading = false
                                Toast.makeText(context, "Student ID '$cleanStudentId' is already registered!", Toast.LENGTH_LONG).show()
                                return@launch
                            }

                            FirebaseAuth.getInstance().createUserWithEmailAndPassword(cleanEmail, password)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val uid = task.result?.user?.uid ?: ""
                                        val user = User(
                                            uid = uid, 
                                            name = cleanName, 
                                            email = cleanEmail,
                                            studentId = cleanStudentId,
                                            role = if (isAdmin) "ADMIN" else "STUDENT",
                                            university = "Premier University",
                                            approvedStatus = "APPROVED"
                                        )
                                        scope.launch {
                                            FirebaseManager.saveUserData(user)
                                            FirebaseAuth.getInstance().signOut()
                                            isLoading = false
                                            Toast.makeText(context, "Registration successful! Please log in.", Toast.LENGTH_LONG).show()
                                            onRegisterSuccess()
                                        }
                                    } else {
                                        isLoading = false
                                        Toast.makeText(context, "Registration Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Create Account", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            TextButton(onClick = onNavigateToLogin) {
                Text("Already have an account? Sign In")
            }
        }
    }
}

@Composable
fun StatusScreen(
    status: String,
    onLogout: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val isBanned = status == "BANNED"
            val icon = if (isBanned) Icons.Default.Block else Icons.Default.Timer
            val tint = if (isBanned) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = tint
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isBanned) "Account Suspended" else "Approval Pending",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = tint
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (isBanned) "Your access has been blocked due to policy violations. Please contact support for more information." 
                       else "Your account is currently under review by the administration. You will be able to access all features once approved.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Logout and Exit", fontWeight = FontWeight.Bold)
            }
        }
    }
}
