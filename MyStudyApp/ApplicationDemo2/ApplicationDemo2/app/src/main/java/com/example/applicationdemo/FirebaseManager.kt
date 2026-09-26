package com.example.applicationdemo

import com.example.applicationdemo.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirebaseManager {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    val currentUser get() = auth.currentUser

    suspend fun getUserData(uid: String): User? {
        return try {
            firestore.collection("users").document(uid).get().await().toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveUserData(user: User) {
        firestore.collection("users").document(user.uid).set(user).await()
    }

    suspend fun isStudentIdRegistered(studentId: String): Boolean {
        return try {
            if (studentId.isBlank()) return false
            val result = firestore.collection("users")
                .whereEqualTo("studentId", studentId)
                .get().await()
            !result.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    // --- Department Management ---

    suspend fun getAllDepartments(): List<Department> {
        return try {
            val depts = firestore.collection("departments").get().await().toObjects(Department::class.java)
            depts.distinctBy { it.name.trim().uppercase() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addDepartment(name: String) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        try {
            val existing = firestore.collection("departments").get().await().toObjects(Department::class.java)
            val alreadyExists = existing.any { it.name.trim().equals(cleanName, ignoreCase = true) }
            if (!alreadyExists) {
                val docRef = firestore.collection("departments").document()
                val department = Department(id = docRef.id, name = cleanName)
                docRef.set(department).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Course Management ---

    suspend fun getCoursesByDepartment(department: String): List<Course> {
        return try {
            firestore.collection("courses")
                .whereEqualTo("department", department)
                .get()
                .await()
                .toObjects(Course::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createCourse(course: Course) {
        val docRef = firestore.collection("courses").document()
        val newCourse = course.copy(id = docRef.id)
        docRef.set(newCourse).await()
    }

    suspend fun forceSeedCseCourses() {
        try {
            val department = "CSE"
            val requiredCourses = listOf("AI", "AIL", "MAD", "OS", "OSL", "ISD", "COA")
            val existingCseCourses = firestore.collection("courses")
                .whereEqualTo("department", department)
                .get().await()
            
            val existingNames = existingCseCourses.documents.mapNotNull { it.getString("name") }
            
            existingCseCourses.documents.forEach { doc ->
                val name = doc.getString("name") ?: ""
                if (name !in requiredCourses) {
                    doc.reference.delete().await()
                }
            }

            requiredCourses.forEach { name ->
                if (name !in existingNames) {
                    createCourse(Course(name = name, department = department, code = name))
                }
            }
        } catch (e: Exception) {}
    }

    // --- Join Requests ---

    suspend fun createJoinRequest(request: JoinRequest) {
        val docRef = firestore.collection("join_requests").document()
        val newRequest = request.copy(id = docRef.id)
        docRef.set(newRequest).await()
    }

    fun observeUserJoinRequests(userId: String, onUpdate: (Map<String, String>) -> Unit): com.google.firebase.firestore.ListenerRegistration {
        return firestore.collection("join_requests")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                val statuses = mutableMapOf<String, String>()
                snapshot?.documents?.forEach { doc ->
                    val courseId = doc.getString("courseId") ?: ""
                    val status = doc.getString("status") ?: "NONE"
                    if (courseId.isNotEmpty()) {
                        statuses[courseId] = status
                    }
                }
                onUpdate(statuses)
            }
    }

    suspend fun updateJoinRequestStatus(requestId: String, status: String) {
        firestore.collection("join_requests").document(requestId).update("status", status).await()
    }

    // --- Feed & Posts ---

    fun observePosts(courseId: String, onUpdate: (List<Post>) -> Unit): com.google.firebase.firestore.ListenerRegistration {
        return firestore.collection("posts")
            .whereEqualTo("courseId", courseId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onUpdate(emptyList())
                    return@addSnapshotListener
                }
                val posts = snapshot?.toObjects(Post::class.java) ?: emptyList()
                val sortedPosts = posts.sortedByDescending { it.timestamp }
                onUpdate(sortedPosts)
            }
    }

    suspend fun createPost(post: Post) {
        val docRef = firestore.collection("posts").document()
        val newPost = post.copy(id = docRef.id)
        docRef.set(newPost).await()
    }

    suspend fun secureDeletePost(postId: String, currentUserId: String): Boolean {
        return try {
            val postDoc = firestore.collection("posts").document(postId).get().await()
            val post = postDoc.toObject(Post::class.java)
            val currentUser = getUserData(currentUserId)
            if ((post?.userId == currentUserId) || (currentUser?.role == "ADMIN")) {
                val comments = firestore.collection("posts").document(postId).collection("comments").get().await()
                comments.documents.forEach { it.reference.delete().await() }
                firestore.collection("posts").document(postId).delete().await()
                true
            } else false
        } catch (e: Exception) { false }
    }

    // --- Comments & Replies ---

    fun observeComments(postId: String, onUpdate: (List<Comment>) -> Unit): com.google.firebase.firestore.ListenerRegistration {
        return firestore.collection("posts").document(postId).collection("comments")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                val comments = snapshot?.toObjects(Comment::class.java) ?: emptyList()
                onUpdate(comments)
            }
    }

    suspend fun addComment(comment: Comment) {
        val batch = firestore.batch()
        val commentDocRef = firestore.collection("posts").document(comment.postId).collection("comments").document()
        val newComment = comment.copy(id = commentDocRef.id)
        
        batch.set(commentDocRef, newComment)
        
        // Increment comment count on the post
        val postRef = firestore.collection("posts").document(comment.postId)
        batch.update(postRef, "commentCount", FieldValue.increment(1))
        
        batch.commit().await()

        // Handle Reply Notification
        if (comment.parentCommentId.isNotEmpty()) {
            try {
                val parentCommentDoc = firestore.collection("posts").document(comment.postId)
                    .collection("comments").document(comment.parentCommentId).get().await()
                val parentComment = parentCommentDoc.toObject(Comment::class.java)
                
                if (parentComment != null && parentComment.userId != comment.userId) {
                    sendNotification(
                        Notification(
                            userId = parentComment.userId,
                            title = "New Reply",
                            message = "${comment.userName} replied to your comment.",
                            courseId = comment.postId, // Using postId as context
                            type = "COMMENT"
                        )
                    )
                }
            } catch (e: Exception) {}
        }
    }

    suspend fun secureDeleteComment(postId: String, commentId: String, currentUserId: String): Boolean {
        return try {
            val commentDoc = firestore.collection("posts").document(postId).collection("comments").document(commentId).get().await()
            val comment = commentDoc.toObject(Comment::class.java)
            val currentUser = getUserData(currentUserId)
            if ((comment?.userId == currentUserId) || (currentUser?.role == "ADMIN")) {
                val batch = firestore.batch()
                batch.delete(firestore.collection("posts").document(postId).collection("comments").document(commentId))
                
                // Decrement comment count
                val postRef = firestore.collection("posts").document(postId)
                batch.update(postRef, "commentCount", FieldValue.increment(-1))
                
                batch.commit().await()
                true
            } else false
        } catch (e: Exception) { false }
    }

    // --- Admin ---

    suspend fun getPendingJoinRequests(): List<JoinRequest> {
        return try {
            firestore.collection("join_requests").whereEqualTo("status", "PENDING").get().await().toObjects(JoinRequest::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getAllJoinRequests(): List<JoinRequest> {
        return try {
            firestore.collection("join_requests").get().await().toObjects(JoinRequest::class.java)
        } catch (e: Exception) { emptyList() }
    }

    // --- Admin Analytics ---

    suspend fun getAdminAnalytics(departmentFilter: String? = null): AdminAnalytics {
        return try {
            val allDepts = getAllDepartments()
            val allRequests = getAllJoinRequests()
            val pending = getPendingJoinRequests()
            val postsQuery = if (departmentFilter != null) {
                firestore.collection("posts").whereEqualTo("department", departmentFilter).get().await()
            } else {
                firestore.collection("posts").get().await()
            }

            val filteredRequests = if (departmentFilter != null) {
                allRequests.filter { it.department.equals(departmentFilter, ignoreCase = true) }
            } else allRequests

            val filteredPending = if (departmentFilter != null) {
                pending.filter { it.department.equals(departmentFilter, ignoreCase = true) }
            } else pending

            AdminAnalytics(
                totalStudents = filteredRequests.map { it.userId }.distinct().size,
                totalDepartments = if (departmentFilter != null) 1 else allDepts.size,
                totalJoinRequests = filteredRequests.size,
                pendingRequests = filteredPending.size,
                totalPosts = postsQuery.size()
            )
        } catch (e: Exception) {
            AdminAnalytics()
        }
    }

    // --- Notifications ---

    fun observeNotifications(userId: String, onUpdate: (List<Notification>) -> Unit): com.google.firebase.firestore.ListenerRegistration {
        return firestore.collection("notifications")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onUpdate(emptyList())
                    return@addSnapshotListener
                }
                val notifications = snapshot?.toObjects(Notification::class.java) ?: emptyList()
                val sorted = notifications.sortedByDescending { it.timestamp }
                onUpdate(sorted)
            }
    }

    fun observeUnreadCount(userId: String, onUpdate: (Int) -> Unit): com.google.firebase.firestore.ListenerRegistration {
        return firestore.collection("notifications")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onUpdate(0)
                    return@addSnapshotListener
                }
                val count = snapshot?.documents?.count { doc ->
                    val notif = doc.toObject(Notification::class.java)
                    notif?.isRead == false
                } ?: 0
                onUpdate(count)
            }
    }

    suspend fun sendNotification(notification: Notification) {
        val docRef = firestore.collection("notifications").document()
        firestore.collection("notifications").document(docRef.id).set(notification.copy(id = docRef.id)).await()
    }

    suspend fun markNotificationAsRead(notificationId: String) {
        try {
            val updates = mapOf<String, Any>(
                "isRead" to true,
                "read" to true
            )
            firestore.collection("notifications").document(notificationId).update(updates).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun markAllAsRead(userId: String) {
        try {
            val allNotifications = firestore.collection("notifications")
                .whereEqualTo("userId", userId)
                .get().await()
            
            val unread = allNotifications.documents.filter { doc ->
                val notif = doc.toObject(Notification::class.java)
                notif?.isRead == false
            }
            
            if (unread.isNotEmpty()) {
                val batch = firestore.batch()
                val updates = mapOf<String, Any>(
                    "isRead" to true,
                    "read" to true
                )
                unread.forEach { batch.update(it.reference, updates) }
                batch.commit().await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getCourseStudents(courseId: String): List<String> {
        return try {
            firestore.collection("join_requests")
                .whereEqualTo("courseId", courseId)
                .whereEqualTo("status", "APPROVED")
                .get().await()
                .documents.mapNotNull { it.getString("userId") }
                .distinct()
        } catch (e: Exception) { emptyList() }
    }

    // --- Saved / Favorite Notes ---

    fun observeSavedNotes(userId: String, onUpdate: (List<SavedNote>) -> Unit): com.google.firebase.firestore.ListenerRegistration {
        return firestore.collection("saved_notes")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onUpdate(emptyList())
                    return@addSnapshotListener
                }
                val notes = snapshot?.toObjects(SavedNote::class.java) ?: emptyList()
                onUpdate(notes)
            }
    }

    fun observeSavedPostIds(userId: String, onUpdate: (Set<String>) -> Unit): com.google.firebase.firestore.ListenerRegistration {
        return firestore.collection("saved_notes")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onUpdate(emptySet())
                    return@addSnapshotListener
                }
                val postIds = snapshot?.documents?.mapNotNull { it.getString("postId") }?.toSet() ?: emptySet()
                onUpdate(postIds)
            }
    }

    suspend fun saveNote(savedNote: SavedNote) {
        try {
            val existing = firestore.collection("saved_notes")
                .whereEqualTo("userId", savedNote.userId)
                .whereEqualTo("postId", savedNote.postId)
                .get().await()
            
            if (existing.isEmpty) {
                val docRef = firestore.collection("saved_notes").document()
                val newNote = savedNote.copy(id = docRef.id)
                docRef.set(newNote).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun removeSavedNote(userId: String, postId: String) {
        try {
            val existing = firestore.collection("saved_notes")
                .whereEqualTo("userId", userId)
                .whereEqualTo("postId", postId)
                .get().await()
            
            val batch = firestore.batch()
            existing.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
