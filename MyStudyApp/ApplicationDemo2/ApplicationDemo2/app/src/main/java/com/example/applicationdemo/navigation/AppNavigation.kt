package com.example.applicationdemo.navigation

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.applicationdemo.FirebaseManager
import com.example.applicationdemo.ProfileScreen
import com.example.applicationdemo.ui.screens.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.net.URLEncoder

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Profile : Screen("profile")
    object Departments : Screen("departments")
    object CourseList : Screen("courses/{deptName}") {
        fun createRoute(deptName: String) = "courses/$deptName"
    }
    object CourseFeed : Screen("feed/{courseId}/{courseName}") {
        fun createRoute(courseId: String, courseName: String) = "feed/$courseId/$courseName"
    }
    object PdfViewer : Screen("pdf_viewer?url={url}&title={title}") {
        fun createRoute(url: String, title: String): String {
            val encodedUrl = try { URLEncoder.encode(url, "UTF-8") } catch (e: Exception) { url }
            val encodedTitle = try { URLEncoder.encode(title, "UTF-8") } catch (e: Exception) { title }
            return "pdf_viewer?url=$encodedUrl&title=$encodedTitle"
        }
    }
    object MyNotes : Screen("my_notes")
    object AdminDashboard : Screen("admin_dashboard?dept={deptName}") {
        fun createRoute(deptName: String? = null) = if (deptName != null) "admin_dashboard?dept=$deptName" else "admin_dashboard"
    }
    object Notifications : Screen("notifications")
    object Status : Screen("status/{status}") {
        fun createRoute(status: String) = "status/$status"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    
    // Check if user is already logged in
    val currentUser = FirebaseAuth.getInstance().currentUser
    val startDest = if (currentUser != null) Screen.Departments.route else Screen.Login.route

    NavHost(navController = navController, startDestination = startDest) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onLoginSuccess = { 
                    navController.navigate(Screen.Departments.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = { 
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Departments.route) {
            DepartmentScreen(
                onNavigateToCourses = { dept -> navController.navigate(Screen.CourseList.createRoute(dept)) },
                onNavigateToAdmin = { navController.navigate(Screen.AdminDashboard.createRoute()) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                onNavigateToMyNotes = { navController.navigate(Screen.MyNotes.route) },
                onLogout = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0)
                    }
                }
            )
        }
        composable(
            route = Screen.CourseList.route,
            arguments = listOf(navArgument("deptName") { type = NavType.StringType })
        ) { backStackEntry ->
            val deptName = backStackEntry.arguments?.getString("deptName") ?: ""
            CourseListScreen(
                department = deptName,
                onNavigateToFeed = { id, name -> navController.navigate(Screen.CourseFeed.createRoute(id, name)) },
                onNavigateToAdmin = { navController.navigate(Screen.AdminDashboard.createRoute(deptName)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.CourseFeed.route,
            arguments = listOf(
                navArgument("courseId") { type = NavType.StringType },
                navArgument("courseName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
            val courseName = backStackEntry.arguments?.getString("courseName") ?: ""
            CourseFeedScreen(
                courseId = courseId,
                courseName = courseName,
                onNavigateToPdfViewer = { url, title ->
                    navController.navigate(Screen.PdfViewer.createRoute(url, title))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.PdfViewer.route,
            arguments = listOf(
                navArgument("url") { type = NavType.StringType; defaultValue = "" },
                navArgument("title") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val rawUrl = backStackEntry.arguments?.getString("url") ?: ""
            val rawTitle = backStackEntry.arguments?.getString("title") ?: ""
            PdfViewerScreen(
                rawUrl = rawUrl,
                rawTitle = rawTitle,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.MyNotes.route) {
            MyNotesScreen(
                onBack = { navController.popBackStack() },
                onNavigateToPdfViewer = { url, title ->
                    navController.navigate(Screen.PdfViewer.createRoute(url, title))
                }
            )
        }
        composable(
            route = Screen.AdminDashboard.route,
            arguments = listOf(navArgument("deptName") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { backStackEntry ->
            val deptName = backStackEntry.arguments?.getString("deptName")
            AdminPanelScreen(
                onBack = { navController.popBackStack() },
                filterDepartment = deptName
            )
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onProfileSaved = { navController.popBackStack() }
            )
        }
        composable(Screen.Notifications.route) {
            NotificationScreen(
                onBack = { navController.popBackStack() },
                onNavigateToCourse = { id, name ->
                    navController.navigate(Screen.CourseFeed.createRoute(id, name))
                }
            )
        }
        composable(
            route = Screen.Status.route,
            arguments = listOf(navArgument("status") { type = NavType.StringType })
        ) { backStackEntry ->
            val status = backStackEntry.arguments?.getString("status") ?: "PENDING"
            StatusScreen(
                status = status,
                onLogout = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0)
                    }
                }
            )
        }
    }
}
