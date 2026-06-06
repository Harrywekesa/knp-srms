package com.example.knpsrms

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.knpsrms.ui.login.LoginScreen
import com.example.knpsrms.ui.student.StudentDashboardScreen
import com.example.knpsrms.ui.lecturer.LecturerDashboardScreen
import com.example.knpsrms.ui.admin.AdminDashboardScreen

@Composable
fun MainNavigation() {
    // Initial route is Login screen
    val backStack = rememberNavBackStack(Login)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            // Login Screen Route
            entry<Login> {
                LoginScreen(
                    onLoginSuccess = { user ->
                        if (user.role == "STUDENT") {
                            backStack.add(StudentDashboard(admissionNo = user.username))
                        } else if (user.role == "LECTURER") {
                            backStack.add(LecturerDashboard(employeeNo = user.username))
                        } else if (user.role == "ADMIN") {
                            backStack.add(AdminDashboard(adminId = user.username))
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Student Dashboard Route
            entry<StudentDashboard> { key ->
                StudentDashboardScreen(
                    admissionNo = key.admissionNo,
                    onLogout = {
                        // Clear backstack and return to Login
                        while (backStack.size > 0) {
                            backStack.removeLastOrNull()
                        }
                        backStack.add(Login)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Lecturer Dashboard Route
            entry<LecturerDashboard> { key ->
                LecturerDashboardScreen(
                    employeeNo = key.employeeNo,
                    onLogout = {
                        // Clear backstack and return to Login
                        while (backStack.size > 0) {
                            backStack.removeLastOrNull()
                        }
                        backStack.add(Login)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Admin Dashboard Route
            entry<AdminDashboard> { key ->
                AdminDashboardScreen(
                    adminId = key.adminId,
                    onLogout = {
                        // Clear backstack and return to Login
                        while (backStack.size > 0) {
                            backStack.removeLastOrNull()
                        }
                        backStack.add(Login)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    )
}
