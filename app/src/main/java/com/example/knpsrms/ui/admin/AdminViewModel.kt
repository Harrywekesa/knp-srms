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

    private val _departments = MutableStateFlow<List<Department>>(emptyList())
    val departments: StateFlow<List<Department>> = _departments.asStateFlow()

    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()

    private val _units = MutableStateFlow<List<CourseUnit>>(emptyList())
    val units: StateFlow<List<CourseUnit>> = _units.asStateFlow()

    private val _userProfiles = MutableStateFlow<List<User>>(emptyList())
    val userProfiles: StateFlow<List<User>> = _userProfiles.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<AuditLog>>(emptyList())
    val auditLogs: StateFlow<List<AuditLog>> = _auditLogs.asStateFlow()

    init {
        loadCurriculum()
        loadUsers()
        loadAuditLogs()
    }

    fun loadCurriculum() {
        viewModelScope.launch {
            try {
                _departments.value = repository.getAllDepartments()
                _courses.value = repository.getAllCourses()
                _units.value = repository.getAllUnits()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadUsers() {
        viewModelScope.launch {
            try {
                _userProfiles.value = repository.getAllUsers()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadAuditLogs() {
        viewModelScope.launch {
            try {
                _auditLogs.value = repository.getAuditLogs()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addDepartment(code: String, name: String) {
        viewModelScope.launch {
            if (repository.addDepartment(code, name)) {
                loadCurriculum()
                loadAuditLogs()
            }
        }
    }

    fun addCourse(code: String, name: String, departmentCode: String) {
        viewModelScope.launch {
            if (repository.addCourse(code, name, departmentCode)) {
                loadCurriculum()
                loadAuditLogs()
            }
        }
    }

    fun addUnit(code: String, name: String, courseCode: String, lecturerId: String?) {
        viewModelScope.launch {
            if (repository.addUnit(code, name, courseCode, lecturerId)) {
                loadCurriculum()
                loadAuditLogs()
            }
        }
    }

    fun addUser(username: String, password: String, role: String, email: String) {
        viewModelScope.launch {
            if (repository.addUser(username, password, role, email)) {
                loadUsers()
                loadAuditLogs()
            }
        }
    }

    fun deleteUser(userId: Int) {
        viewModelScope.launch {
            if (repository.deleteUser(userId)) {
                loadUsers()
                loadAuditLogs()
                val currentState = _uiState.value
                if (currentState is AdminUiState.Success) {
                    val currentSelected = currentState.selectedStudent
                    val students = repository.getAllStudents()
                    if (currentSelected != null) {
                        val userObj = _userProfiles.value.find { it.id == userId }
                        if (userObj != null && userObj.username == currentSelected.admissionNo) {
                            clearSelectedStudent()
                        }
                    }
                    _uiState.update { currentState.copy(students = students) }
                }
            }
        }
    }

    fun enrollStudentInUnit(studentId: String, unitCode: String) {
        viewModelScope.launch {
            if (repository.enrollStudentInUnit(studentId, unitCode)) {
                val student = repository.getStudent(studentId)
                if (student != null) selectStudent(student)
                loadAuditLogs()
            }
        }
    }

    fun updateStudentGrade(studentId: String, unitCode: String, catMark: Double?, examMark: Double?) {
        viewModelScope.launch {
            if (repository.updateStudentGrade(studentId, unitCode, catMark, examMark)) {
                val student = repository.getStudent(studentId)
                if (student != null) selectStudent(student)
                loadAuditLogs()
            }
        }
    }

    fun recordAttendance(studentId: String, unitCode: String, date: String, status: String) {
        viewModelScope.launch {
            if (repository.recordAttendance(studentId, unitCode, date, status)) {
                val student = repository.getStudent(studentId)
                if (student != null) selectStudent(student)
                loadAuditLogs()
            }
        }
    }

    fun recordFeePayment(
        studentId: String,
        amount: Double,
        receiptNo: String,
        date: String,
        description: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val success = repository.recordFeePayment(studentId, amount, receiptNo, date, description)
            if (success) {
                val student = repository.getStudent(studentId)
                if (student != null) selectStudent(student)
                loadAuditLogs()
            }
            onResult(success)
        }
    }

    fun recordTuitionInvoice(studentId: String, amount: Double, date: String, description: String) {
        viewModelScope.launch {
            if (repository.recordTuitionInvoice(studentId, amount, date, description)) {
                val student = repository.getStudent(studentId)
                if (student != null) selectStudent(student)
                loadAuditLogs()
            }
        }
    }

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
