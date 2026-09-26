package com.example.applicationdemo.ui.screens

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.applicationdemo.CloudinaryHelper
import com.example.applicationdemo.FirebaseManager
import com.example.applicationdemo.model.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseFeedScreen(
    courseId: String,
    courseName: String,
    onNavigateToPdfViewer: (String, String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var savedPostIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }
    var currentUser by remember { mutableStateOf<User?>(null) }
    
    var searchQuery by remember { mutableStateOf("") }
    var showOnlyWithAttachments by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }

    var selectedPostForComments by remember { mutableStateOf<Post?>(null) }
    val sheetState = rememberModalBottomSheetState()
    var showCommentsSheet by remember { mutableStateOf(false) }
    var showAddPostDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val uid = FirebaseManager.currentUser?.uid
        if (uid != null) {
            currentUser = FirebaseManager.getUserData(uid)
        }
    }

    DisposableEffect(currentUser?.uid) {
        val uid = currentUser?.uid
        if (uid != null) {
            val listener = FirebaseManager.observeSavedPostIds(uid) { ids ->
                savedPostIds = ids
            }
            onDispose { listener.remove() }
        } else {
            onDispose { }
        }
    }

    DisposableEffect(courseId) {
        isLoading = true 
        val listener = FirebaseManager.observePosts(courseId) { updatedPosts ->
            posts = updatedPosts.sortedByDescending { it.timestamp }
            isLoading = false
        }
        onDispose {
            listener.remove()
        }
    }

    val filteredPosts = remember(posts, searchQuery, showOnlyWithAttachments) {
        posts.filter { post ->
            val matchesSearch = post.title.contains(searchQuery, ignoreCase = true) || 
                               post.description.contains(searchQuery, ignoreCase = true) ||
                               post.userName.contains(searchQuery, ignoreCase = true)
            val matchesAttachmentFilter = !showOnlyWithAttachments || post.fileUrl.isNotEmpty()
            matchesSearch && matchesAttachmentFilter
        }
    }

    if (showCommentsSheet && selectedPostForComments != null) {
        ModalBottomSheet(
            onDismissRequest = { showCommentsSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            CommentsSection(post = selectedPostForComments!!, currentUser = currentUser)
        }
    }

    if (showAddPostDialog) {
        AddPostDialog(
            onDismiss = { showAddPostDialog = false },
            onSubmit = { title, desc, fileUri ->
                scope.launch {
                    try {
                        var finalUrl = ""
                        if (fileUri != null) {
                            finalUrl = CloudinaryHelper.uploadFile(context, fileUri) ?: ""
                        }
                        if (fileUri != null && finalUrl.isEmpty()) {
                            Toast.makeText(context, "Upload failed!", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        val newPost = Post(
                            courseId = courseId,
                            department = currentUser?.department ?: "",
                            userId = currentUser?.uid ?: "",
                            userName = currentUser?.name ?: "User",
                            title = title,
                            description = desc,
                            fileUrl = finalUrl,
                            timestamp = System.currentTimeMillis()
                        )
                        FirebaseManager.createPost(newPost)
                        
                        // Notify all students in this course
                        scope.launch {
                            val studentIds = FirebaseManager.getCourseStudents(courseId)
                            studentIds.forEach { targetId ->
                                if (targetId != currentUser?.uid) {
                                    FirebaseManager.sendNotification(
                                        Notification(
                                            userId = targetId,
                                            title = "New Post in $courseName",
                                            message = "${currentUser?.name} shared a new material: $title",
                                            courseId = courseId,
                                            courseName = courseName,
                                            type = "POST"
                                        )
                                    )
                                }
                            }
                        }

                        showAddPostDialog = false
                    } catch (e: Exception) {
                        Log.e("DriveUpload", "Failed to share post", e)
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { 
                        if (isSearchActive) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search notes or users...") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                textStyle = MaterialTheme.typography.bodyLarge
                            )
                        } else {
                            Column {
                                Text(courseName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("Shared Materials", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
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
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !showOnlyWithAttachments,
                        onClick = { showOnlyWithAttachments = false },
                        label = { Text("All Posts") },
                        leadingIcon = if (!showOnlyWithAttachments) {
                            { Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = showOnlyWithAttachments,
                        onClick = { showOnlyWithAttachments = true },
                        label = { Text("With Files") },
                        leadingIcon = if (showOnlyWithAttachments) {
                            { Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddPostDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Add note")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (filteredPosts.isEmpty()) {
                EmptyFeedPlaceholder(isSearch = isSearchActive || showOnlyWithAttachments)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredPosts, key = { it.id }) { post ->
                        val isSaved = savedPostIds.contains(post.id)
                        PostCard(
                            post = post,
                            currentUser = currentUser,
                            isSaved = isSaved,
                            onToggleSave = {
                                val uid = currentUser?.uid ?: return@PostCard
                                scope.launch {
                                    if (isSaved) {
                                        FirebaseManager.removeSavedNote(uid, post.id)
                                        Toast.makeText(context, "Removed from My Notes", Toast.LENGTH_SHORT).show()
                                    } else {
                                        FirebaseManager.saveNote(
                                            SavedNote(
                                                userId = uid,
                                                postId = post.id,
                                                courseId = courseId,
                                                courseName = courseName,
                                                title = post.title,
                                                description = post.description,
                                                fileUrl = post.fileUrl,
                                                timestamp = post.timestamp
                                            )
                                        )
                                        Toast.makeText(context, "Added to My Notes!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onOpenComments = {
                                selectedPostForComments = post
                                showCommentsSheet = true
                            },
                            onOpenAttachment = { url, title ->
                                onNavigateToPdfViewer(url, title)
                            },
                            onDeletePost = {
                                scope.launch {
                                    FirebaseManager.secureDeletePost(post.id, currentUser?.uid ?: "")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PostCard(
    post: Post, 
    currentUser: User?,
    isSaved: Boolean,
    onToggleSave: () -> Unit,
    onOpenComments: () -> Unit,
    onOpenAttachment: (String, String) -> Unit,
    onDeletePost: () -> Unit
) {
    val canDelete = currentUser?.role == "ADMIN" || currentUser?.uid == post.userId
    val dateStr = remember(post.timestamp) {
        val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
        sdf.format(Date(post.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(post.userName.firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = post.userName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(text = dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }

                IconButton(onClick = onToggleSave) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Save Note",
                        tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }

                if (canDelete) {
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, contentDescription = null) }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = { showMenu = false; onDeletePost() },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(text = post.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (post.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = post.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (post.fileUrl.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable {
                        onOpenAttachment(post.fileUrl, post.title)
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFD32F2F))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("View Attached Material", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth().clickable { onOpenComments() }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (post.commentCount > 0) "Comments (${post.commentCount})" else "Comments",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun EmptyFeedPlaceholder(isSearch: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Inbox, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = if (isSearch) "No matches found" else "No posts yet", color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun AddPostDialog(onDismiss: () -> Unit, onSubmit: (String, String, Uri?) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { selectedUri = it }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Announcement", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Details") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), minLines = 3)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { picker.launch("*/*") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), enabled = !isSubmitting) {
                    Icon(Icons.Default.AttachFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (selectedUri != null) "File Attached" else "Attach Material")
                }
            }
        },
        confirmButton = { 
            Button(
                onClick = { 
                    if (title.isNotBlank() && !isSubmitting) {
                        isSubmitting = true
                        onSubmit(title, description, selectedUri) 
                    }
                }, 
                enabled = title.isNotBlank() && !isSubmitting
            ) { 
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Post")
                }
            } 
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isSubmitting) { Text("Cancel") } }
    )
}

@Composable
fun CommentsSection(post: Post, currentUser: User?) {
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var newCommentText by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<Comment?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val currentUserId = currentUser?.uid ?: ""

    DisposableEffect(post.id) {
        val listener = FirebaseManager.observeComments(post.id) { updatedComments ->
            comments = updatedComments
            isLoading = false
        }
        onDispose { listener.remove() }
    }

    Column(modifier = Modifier.fillMaxHeight(0.85f).padding(16.dp).imePadding()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Comments", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (post.commentCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(post.commentCount.toString(), modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            val topLevelComments = comments.filter { it.parentCommentId.isEmpty() }
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(topLevelComments) { comment ->
                    CommentItem(
                        comment = comment,
                        replies = comments.filter { it.parentCommentId == comment.id },
                        currentUserId = currentUserId,
                        isAdmin = currentUser?.role == "ADMIN",
                        onReply = { replyingTo = comment },
                        onDelete = { scope.launch { FirebaseManager.secureDeleteComment(post.id, it.id, currentUserId) } }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        if (replyingTo != null) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Replying to ${replyingTo!!.userName}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = { replyingTo = null }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newCommentText,
                onValueChange = { newCommentText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(if (replyingTo != null) "Write a reply..." else "Add comment...") },
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (newCommentText.isNotBlank()) {
                        scope.launch {
                            FirebaseManager.addComment(Comment(
                                postId = post.id,
                                userId = currentUserId,
                                userName = currentUser?.name ?: "User",
                                content = newCommentText,
                                parentCommentId = replyingTo?.id ?: ""
                            ))
                            newCommentText = ""
                            replyingTo = null
                        }
                    }
                },
                enabled = newCommentText.isNotBlank()
            ) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    replies: List<Comment>,
    currentUserId: String,
    isAdmin: Boolean,
    onReply: () -> Unit,
    onDelete: (Comment) -> Unit
) {
    Column {
        Row {
            Surface(modifier = Modifier.size(32.dp), shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                Box(contentAlignment = Alignment.Center) { Text(comment.userName.firstOrNull()?.uppercase() ?: "?") }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Surface(shape = RoundedCornerShape(0.dp, 12.dp, 12.dp, 12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(comment.userName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        Text(comment.content, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onReply, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(30.dp)) {
                        Text("Reply", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    if (isAdmin || comment.userId == currentUserId) {
                        Spacer(modifier = Modifier.width(16.dp))
                        TextButton(onClick = { onDelete(comment) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(30.dp)) {
                            Text("Delete", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        
        // Render Replies
        if (replies.isNotEmpty()) {
            Column(modifier = Modifier.padding(start = 40.dp, top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                replies.forEach { reply ->
                    Row {
                        Surface(modifier = Modifier.size(24.dp), shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer) {
                            Box(contentAlignment = Alignment.Center) { Text(reply.userName.firstOrNull()?.uppercase() ?: "?", fontSize = 10.sp) }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(shape = RoundedCornerShape(0.dp, 10.dp, 10.dp, 10.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(reply.userName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                    Text(reply.content, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            if (isAdmin || reply.userId == currentUserId) {
                                TextButton(onClick = { onDelete(reply) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(24.dp)) {
                                    Text("Delete", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
