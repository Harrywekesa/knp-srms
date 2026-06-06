package com.example.knpsrms.ui.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.knpsrms.data.DataRepository
import com.example.knpsrms.data.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface StudentUiState {
    object Loading : StudentUiState
    data class Success(
        val student: Student,
        val grades: List<Grade>,
        val feeStatement: FeeStatement,
        val attendance: List<AttendanceSummary>
    ) : StudentUiState
    data class Error(val message: String) : StudentUiState
}

class StudentViewModel(private val repository: DataRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<StudentUiState>(StudentUiState.Loading)
    val uiState: StateFlow<StudentUiState> = _uiState

    fun loadStudentData(admissionNo: String) {
        _uiState.value = StudentUiState.Loading
        viewModelScope.launch {
            try {
                val student = repository.getStudent(admissionNo)
                if (student != null) {
                    val grades = repository.getStudentGrades(admissionNo)
                    val feeStatement = repository.getStudentFeeStatement(admissionNo)
                    val attendance = repository.getStudentAttendanceSummary(admissionNo)
                    _uiState.value = StudentUiState.Success(
                        student = student,
                        grades = grades,
                        feeStatement = feeStatement,
                        attendance = attendance
                    )
                } else {
                    _uiState.value = StudentUiState.Error("Student profile not found in offline records.")
                }
            } catch (e: Exception) {
                _uiState.value = StudentUiState.Error("Database query failed: ${e.message}")
            }
        }
    }
}
