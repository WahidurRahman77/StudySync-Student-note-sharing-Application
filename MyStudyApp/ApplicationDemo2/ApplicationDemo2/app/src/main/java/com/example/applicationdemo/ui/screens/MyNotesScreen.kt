package com.example.applicationdemo.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.applicationdemo.FirebaseManager
import com.example.applicationdemo.model.SavedNote
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class NoteSortOption(val label: String) {
    NEWEST("Newest First"),
    OLDEST("Oldest First"),
    TITLE_AZ("Title A-Z"),
    TITLE_ZA("Title Z-A")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyNotesScreen(
    onBack: () -> Unit,
    onNavigateToPdfViewer: (String, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userId = FirebaseManager.currentUser?.uid ?: ""

    var savedNotes by remember { mutableStateOf<List<SavedNote>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedCourseFolder by remember { mutableStateOf<String?>(null) }
    
    // Search & Sort State
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedSort by remember { mutableStateOf(NoteSortOption.NEWEST) }
    var showSortMenu by remember { mutableStateOf(false) }

    DisposableEffect(userId) {
        val listener = FirebaseManager.observeSavedNotes(userId) { notes ->
            savedNotes = notes
            isLoading = false
        }
        onDispose { listener.remove() }
    }

    // Group notes by Course Name
    val courseFolders = remember(savedNotes, searchQuery) {
        savedNotes
            .filter { 
                it.courseName.contains(searchQuery, ignoreCase = true) ||
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
            }
            .groupBy { it.courseName }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search saved notes...") },
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
                        Column {
                            Text(
                                text = selectedCourseFolder ?: "My Saved Notes",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (selectedCourseFolder != null) "Course Folder" else "${savedNotes.size} Saved Materials",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSearchActive) {
                            isSearchActive = false
                            searchQuery = ""
                        } else if (selectedCourseFolder != null) {
                            selectedCourseFolder = null
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        if (selectedCourseFolder != null) {
                            Box {
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(Icons.Default.Sort, contentDescription = "Sort")
                                }
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    NoteSortOption.values().forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option.label) },
                                            onClick = {
                                                selectedSort = option
                                                showSortMenu = false
                                            },
                                            leadingIcon = if (selectedSort == option) {
                                                { Icon(Icons.Default.Check, contentDescription = null) }
                                            } else null
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (savedNotes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No saved notes yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Click the bookmark icon on any post in the course feed to save it here for quick access.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else if (selectedCourseFolder == null) {
                // Folder List View (Grouped by Course)
                if (courseFolders.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No matching folders found", color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(courseFolders.keys.toList()) { courseName ->
                            val folderNotes = courseFolders[courseName] ?: emptyList()
                            CourseFolderCard(
                                courseName = courseName,
                                noteCount = folderNotes.size,
                                onClick = { selectedCourseFolder = courseName }
                            )
                        }
                    }
                }
            } else {
                // Inside Specific Course Folder View
                val folderNotes = remember(savedNotes, selectedCourseFolder, searchQuery, selectedSort) {
                    val raw = savedNotes.filter { 
                        it.courseName == selectedCourseFolder &&
                        (it.title.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true))
                    }
                    when (selectedSort) {
                        NoteSortOption.NEWEST -> raw.sortedByDescending { it.timestamp }
                        NoteSortOption.OLDEST -> raw.sortedBy { it.timestamp }
                        NoteSortOption.TITLE_AZ -> raw.sortedBy { it.title.lowercase() }
                        NoteSortOption.TITLE_ZA -> raw.sortedByDescending { it.title.lowercase() }
                    }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Sorting Indicator Chip Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sorted by: ${selectedSort.label}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                    if (folderNotes.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No notes in this folder", color = MaterialTheme.colorScheme.outline)
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(folderNotes, key = { it.id }) { note ->
                                SavedNoteCard(
                                    savedNote = note,
                                    onView = { onNavigateToPdfViewer(note.fileUrl, note.title) },
                                    onRemove = {
                                        scope.launch {
                                            FirebaseManager.removeSavedNote(userId, note.postId)
                                            Toast.makeText(context, "Removed from My Notes", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CourseFolderCard(
    courseName: String,
    noteCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            Column {
                Text(
                    text = courseName.ifEmpty { "General" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$noteCount ${if (noteCount == 1) "Note" else "Notes"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun SavedNoteCard(
    savedNote: SavedNote,
    onView: () -> Unit,
    onRemove: () -> Unit
) {
    val dateStr = remember(savedNote.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        sdf.format(Date(savedNote.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = savedNote.courseName.ifEmpty { "Course Material" },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "Remove Bookmark",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = savedNote.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (savedNote.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = savedNote.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                if (savedNote.fileUrl.isNotEmpty()) {
                    Button(
                        onClick = onView,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("View Note", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
