package com.example.applicationdemo

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@Composable
fun ProfileUploadScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isUploading by remember { mutableStateOf(false) }
    var uploadedImageUrl by remember { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            isUploading = true
            scope.launch {
                // CloudinaryHelper ব্যবহার করে ছবি আপলোড
                val url = CloudinaryHelper.uploadFile(context, selectedUri)
                if (url != null) {
                    uploadedImageUrl = url
                    saveUrlToFirestore(
                        imageUrl = url,
                        onSuccess = {
                            isUploading = false
                            Toast.makeText(context, "ছবি সফলভাবে আপলোড হয়েছে!", Toast.LENGTH_SHORT).show()
                        },
                        onError = { error ->
                            isUploading = false
                            Toast.makeText(context, "Firestore Error: $error", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    isUploading = false
                    Toast.makeText(context, "Cloudinary Upload Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isUploading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text("ছবি আপলোড হচ্ছে, অপেক্ষা করুন...")
        } else {
            Button(onClick = {
                imagePickerLauncher.launch("image/*")
            }) {
                Text("গ্যালারি থেকে ছবি সিলেক্ট করো")
            }
        }

        if (uploadedImageUrl.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Uploaded URL: $uploadedImageUrl",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

fun saveUrlToFirestore(
    imageUrl: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseManager.currentUser?.uid ?: "user_123"

    val data = hashMapOf(
        "profileImage" to imageUrl,
        "updatedAt" to System.currentTimeMillis()
    )

    db.collection("users").document(uid)
        .update(data as Map<String, Any>)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { e -> 
            // If document doesn't exist, use set
            db.collection("users").document(uid).set(data)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { err -> onError(err.message ?: "Unknown error") }
        }
}
