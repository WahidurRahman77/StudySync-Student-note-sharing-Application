package com.example.applicationdemo.model

import com.google.firebase.firestore.PropertyName

data class Department(
    val id: String = "",
    val name: String = ""
)

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val studentId: String = "",
    val role: String = "STUDENT", // STUDENT, ADMIN
    val profileImage: String = "",
    val department: String = "",
    val approvedStatus: String = "APPROVED", // General app access
    val university: String = "Premier University",
    val activeCourses: String = ""
)

data class Course(
    val id: String = "",
    val name: String = "",
    val code: String = "",
    val department: String = "" // CSE, EEE, etc.
)

data class JoinRequest(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val courseId: String = "",
    val courseName: String = "",
    val department: String = "",
    val status: String = "PENDING" // PENDING, APPROVED, DECLINED
)

data class Post(
    val id: String = "",
    val courseId: String = "",
    val department: String = "",
    val userId: String = "",
    val userName: String = "",
    val title: String = "",
    val description: String = "",
    val fileUrl: String = "", // Google Drive webViewLink
    val timestamp: Long = System.currentTimeMillis(),
    val commentCount: Int = 0
)

data class Comment(
    val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val userName: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val parentCommentId: String = "" // Empty if top-level, otherwise points to parent comment
)

data class Notification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "POST", // POST, COMMENT, APPROVAL
    val courseId: String = "",
    val courseName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    @get:PropertyName("isRead") @set:PropertyName("isRead") var isRead: Boolean = false
)

data class SavedNote(
    val id: String = "",
    val userId: String = "",
    val postId: String = "",
    val courseId: String = "",
    val courseName: String = "",
    val title: String = "",
    val description: String = "",
    val fileUrl: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class AdminAnalytics(
    val totalStudents: Int = 0,
    val totalDepartments: Int = 0,
    val totalJoinRequests: Int = 0,
    val pendingRequests: Int = 0,
    val totalPosts: Int = 0
)
