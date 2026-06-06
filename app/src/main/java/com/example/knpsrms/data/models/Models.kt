package com.example.knpsrms.data.models

data class User(
    val id: Int,
    val username: String,
    val role: String,
    val email: String
)

data class Student(
    val admissionNo: String,
    val firstName: String,
    val lastName: String,
    val gender: String,
    val dob: String,
    val phone: String,
    val courseCode: String,
    val feeBalance: Double,
    val nationalId: String = "",
    val postalAddress: String = "",
    val nextOfKinName: String = "",
    val nextOfKinRelationship: String = "",
    val nextOfKinPhone: String = "",
    val enrollmentStatus: String = "Active",
    val currentYear: Int = 1,
    val graduationCleared: Boolean = false
) {
    val fullName: String get() = "$firstName $lastName"
}

data class Lecturer(
    val employeeNo: String,
    val firstName: String,
    val lastName: String,
    val department: String
) {
    val fullName: String get() = "$firstName $lastName"
}

data class Grade(
    val unitCode: String,
    val unitName: String,
    val catMark: Double?,
    val examMark: Double?
) {
    val aggregateMark: Double? get() {
        if (catMark == null && examMark == null) return null
        return (catMark ?: 0.0) + (examMark ?: 0.0)
    }

    val gradeLetter: String get() {
        val score = aggregateMark ?: return "N/A"
        return when {
            score >= 70.0 -> "A"
            score >= 60.0 -> "B"
            score >= 50.0 -> "C"
            score >= 40.0 -> "PASS"
            else -> "FAIL"
        }
    }
}

data class AttendanceRecord(
    val unitCode: String,
    val unitName: String,
    val date: String,
    val status: String
)

data class AttendanceSummary(
    val unitCode: String,
    val unitName: String,
    val totalClasses: Int,
    val presentCount: Int
) {
    val percentage: Double get() {
        if (totalClasses == 0) return 0.0
        return (presentCount.toDouble() / totalClasses.toDouble()) * 100.0
    }
}

data class FeeStatementItem(
    val id: Int,
    val type: String, // "INVOICE" or "PAYMENT"
    val amount: Double,
    val date: String,
    val description: String,
    val receiptNo: String?
)

data class FeeStatement(
    val items: List<FeeStatementItem>,
    val totalInvoiced: Double,
    val totalPaid: Double,
    val balance: Double
)

data class StudentRosterItem(
    val enrollmentId: Int,
    val studentId: String,
    val studentName: String,
    val catMark: Double?,
    val examMark: Double?,
    val presentCount: Int,
    val totalClasses: Int
)

data class DisciplinaryRecord(
    val id: Int,
    val studentId: String,
    val date: String,
    val warningLevel: String,
    val description: String,
    val status: String
)

data class ExtracurricularActivity(
    val id: Int,
    val studentId: String,
    val activityName: String,
    val role: String,
    val year: String
)

data class OfficialCommunication(
    val id: Int,
    val studentId: String,
    val date: String,
    val documentType: String,
    val title: String,
    val content: String
)

data class PortfolioOfEvidence(
    val id: Int,
    val studentId: String,
    val unitCode: String,
    val score: Double,
    val assessorName: String,
    val status: String
)

data class FinancialAid(
    val id: Int,
    val studentId: String,
    val amount: Double,
    val source: String,
    val allocationDate: String
)

