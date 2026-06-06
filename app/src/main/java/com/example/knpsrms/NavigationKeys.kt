package com.example.knpsrms

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object Login : NavKey

@Serializable
data class StudentDashboard(val admissionNo: String) : NavKey

@Serializable
data class LecturerDashboard(val employeeNo: String) : NavKey

@Serializable
data class AdminDashboard(val adminId: String) : NavKey

