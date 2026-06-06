package com.example.knpsrms.ui.lecturer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.knpsrms.data.DataRepository
import com.example.knpsrms.data.models.Lecturer
import com.example.knpsrms.data.models.StudentRosterItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface LecturerUiState {
    object Loading : LecturerUiState
    data class Success(
        val lecturer: Lecturer,
        val units: List<DataRepository.UnitEntity>,
        val selectedUnitCode: String?,
        val roster: List<StudentRosterItem>
    ) : LecturerUiState
    data class Error(val message: String) : LecturerUiState
}

class LecturerViewModel(private val repository: DataRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<LecturerUiState>(LecturerUiState.Loading)
    val uiState: StateFlow<LecturerUiState> = _uiState

    private var currentEmployeeNo = ""
    private var cachedLecturer: Lecturer? = null
    private var cachedUnits = listOf<DataRepository.UnitEntity>()

    fun loadLecturerData(employeeNo: String) {
        currentEmployeeNo = employeeNo
        _uiState.value = LecturerUiState.Loading
        viewModelScope.launch {
            try {
                val lecturer = repository.getLecturer(employeeNo)
                if (lecturer != null) {
                    cachedLecturer = lecturer
                    val units = repository.getLecturerUnits(employeeNo)
                    cachedUnits = units
                    _uiState.value = LecturerUiState.Success(
                        lecturer = lecturer,
                        units = units,
                        selectedUnitCode = null,
                        roster = emptyList()
                    )
                } else {
                    _uiState.value = LecturerUiState.Error("Lecturer staff profile not found in offline records.")
                }
            } catch (e: Exception) {
                _uiState.value = LecturerUiState.Error("Database query failed: ${e.message}")
            }
        }
    }

    fun selectUnit(unitCode: String) {
        val currentState = _uiState.value
        if (currentState is LecturerUiState.Success) {
            viewModelScope.launch {
                try {
                    val roster = repository.getStudentRoster(unitCode)
                    _uiState.value = currentState.copy(
                        selectedUnitCode = unitCode,
                        roster = roster
                    )
                } catch (e: Exception) {
                    _uiState.value = LecturerUiState.Error("Failed to load student roster: ${e.message}")
                }
            }
        }
    }

    fun submitGrades(enrollmentId: Int, catMark: Double?, examMark: Double?, unitCode: String) {
        viewModelScope.launch {
            try {
                repository.submitGrades(enrollmentId, catMark, examMark)
                // Reload roster
                selectUnit(unitCode)
            } catch (e: Exception) {
                _uiState.value = LecturerUiState.Error("Failed to save student grade: ${e.message}")
            }
        }
    }

    fun recordAttendance(enrollmentId: Int, status: String, unitCode: String) {
        viewModelScope.launch {
            try {
                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                repository.recordAttendance(enrollmentId, todayDate, status, currentEmployeeNo)
                // Reload roster
                selectUnit(unitCode)
            } catch (e: Exception) {
                _uiState.value = LecturerUiState.Error("Failed to log attendance: ${e.message}")
            }
        }
    }

    fun clearSelectedUnit() {
        val currentState = _uiState.value
        if (currentState is LecturerUiState.Success) {
            _uiState.value = currentState.copy(
                selectedUnitCode = null,
                roster = emptyList()
            )
        }
    }
}
