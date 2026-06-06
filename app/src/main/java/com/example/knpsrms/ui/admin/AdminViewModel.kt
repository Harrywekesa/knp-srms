package com.example.knpsrms.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.knpsrms.data.DataRepository
import com.example.knpsrms.data.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStream

sealed interface AdminUiState {
    object Loading : AdminUiState
    data class Success(
        val students: List<Student>,
        val selectedStudent: Student? = null,
        val grades: List<Grade> = emptyList(),
        val feeStatement: FeeStatement? = null,
        val attendance: List<AttendanceSummary> = emptyList(),
        val disciplinaryRecords: List<DisciplinaryRecord> = emptyList(),
        val extracurricularActivities: List<ExtracurricularActivity> = emptyList(),
        val officialCommunications: List<OfficialCommunication> = emptyList(),
        val portfolios: List<PortfolioOfEvidence> = emptyList(),
        val financialAid: List<FinancialAid> = emptyList(),
        val csvImportStatus: String? = null
    ) : AdminUiState
    data class Error(val message: String) : AdminUiState
}

class AdminViewModel(private val repository: DataRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminUiState>(AdminUiState.Loading)
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    fun loadAllStudents() {
        viewModelScope.launch {
            _uiState.update { AdminUiState.Loading }
            try {
                val students = repository.getAllStudents()
                _uiState.update { AdminUiState.Success(students = students) }
            } catch (e: Exception) {
                _uiState.update { AdminUiState.Error(e.message ?: "Failed to load student roster") }
            }
        }
    }

    fun selectStudent(student: Student) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is AdminUiState.Success) {
                try {
                    val grades = repository.getStudentGrades(student.admissionNo)
                    val feeStatement = repository.getStudentFeeStatement(student.admissionNo)
                    val attendance = repository.getStudentAttendanceSummary(student.admissionNo)
                    val disc = repository.getDisciplinaryRecords(student.admissionNo)
                    val extra = repository.getExtracurricularActivities(student.admissionNo)
                    val comm = repository.getOfficialCommunications(student.admissionNo)
                    val poe = repository.getPortfolioOfEvidences(student.admissionNo)
                    val aid = repository.getFinancialAidRecords(student.admissionNo)

                    _uiState.update {
                        currentState.copy(
                            selectedStudent = student,
                            grades = grades,
                            feeStatement = feeStatement,
                            attendance = attendance,
                            disciplinaryRecords = disc,
                            extracurricularActivities = extra,
                            officialCommunications = comm,
                            portfolios = poe,
                            financialAid = aid,
                            csvImportStatus = null
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update { AdminUiState.Error(e.message ?: "Failed to load student folders") }
                }
            }
        }
    }

    fun clearSelectedStudent() {
        val currentState = _uiState.value
        if (currentState is AdminUiState.Success) {
            _uiState.update {
                currentState.copy(
                    selectedStudent = null,
                    grades = emptyList(),
                    feeStatement = null,
                    attendance = emptyList(),
                    disciplinaryRecords = emptyList(),
                    extracurricularActivities = emptyList(),
                    officialCommunications = emptyList(),
                    portfolios = emptyList(),
                    financialAid = emptyList()
                )
            }
        }
    }

    fun addOrUpdateStudent(student: Student) {
        viewModelScope.launch {
            try {
                val success = repository.addOrUpdateStudent(student)
                if (success) {
                    val students = repository.getAllStudents()
                    val currentState = _uiState.value
                    if (currentState is AdminUiState.Success) {
                        _uiState.update { currentState.copy(students = students) }
                    } else {
                        _uiState.update { AdminUiState.Success(students = students) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { AdminUiState.Error(e.message ?: "Failed to enroll student") }
            }
        }
    }

    fun addDisciplinaryRecord(studentId: String, warningLevel: String, description: String, date: String, status: String) {
        viewModelScope.launch {
            try {
                val success = repository.addDisciplinaryRecord(studentId, warningLevel, description, date, status)
                if (success) {
                    val student = repository.getStudent(studentId)
                    if (student != null) selectStudent(student)
                }
            } catch (e: Exception) {
                _uiState.update { AdminUiState.Error(e.message ?: "Failed to add disciplinary entry") }
            }
        }
    }

    fun addOfficialCommunication(studentId: String, documentType: String, title: String, content: String, date: String) {
        viewModelScope.launch {
            try {
                val success = repository.addOfficialCommunication(studentId, documentType, title, content, date)
                if (success) {
                    val student = repository.getStudent(studentId)
                    if (student != null) selectStudent(student)
                }
            } catch (e: Exception) {
                _uiState.update { AdminUiState.Error(e.message ?: "Failed to log official memo") }
            }
        }
    }

    fun addPortfolioOfEvidence(studentId: String, unitCode: String, score: Double, assessorName: String, status: String) {
        viewModelScope.launch {
            try {
                val success = repository.addPortfolioOfEvidence(studentId, unitCode, score, assessorName, status)
                if (success) {
                    val student = repository.getStudent(studentId)
                    if (student != null) selectStudent(student)
                }
            } catch (e: Exception) {
                _uiState.update { AdminUiState.Error(e.message ?: "Failed to add portfolio score") }
            }
        }
    }

    fun addFinancialAid(studentId: String, amount: Double, source: String, date: String) {
        viewModelScope.launch {
            try {
                val success = repository.addFinancialAid(studentId, amount, source, date)
                if (success) {
                    val student = repository.getStudent(studentId)
                    if (student != null) selectStudent(student)
                }
            } catch (e: Exception) {
                _uiState.update { AdminUiState.Error(e.message ?: "Failed to record financial aid allocation") }
            }
        }
    }

    fun importCsv(inputStream: InputStream) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is AdminUiState.Success) {
                _uiState.update { currentState.copy(csvImportStatus = "Importing...") }
                try {
                    val count = repository.importStudentsFromCsv(inputStream)
                    val students = repository.getAllStudents()
                    _uiState.update {
                        currentState.copy(
                            students = students,
                            csvImportStatus = "Successfully imported $count student records from CSV!"
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update { currentState.copy(csvImportStatus = "Error: ${e.message}") }
                }
            }
        }
    }
    
    fun clearImportStatus() {
        val currentState = _uiState.value
        if (currentState is AdminUiState.Success) {
            _uiState.update { currentState.copy(csvImportStatus = null) }
        }
    }
}
