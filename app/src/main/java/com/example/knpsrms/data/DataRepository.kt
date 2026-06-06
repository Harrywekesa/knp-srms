package com.example.knpsrms.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.knpsrms.data.local.SrmsDbHelper
import com.example.knpsrms.data.models.*

class DataRepository(context: Context) {
    private val dbHelper = SrmsDbHelper(context)

    // User Authentication
    fun login(username: String, password: String, role: String): User? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT id, username, role, email FROM users WHERE username = ? AND password = ? AND role = ?",
            arrayOf(username, password, role)
        )
        var user: User? = null
        if (cursor.moveToFirst()) {
            user = User(
                id = cursor.getInt(0),
                username = cursor.getString(1),
                role = cursor.getString(2),
                email = cursor.getString(3)
            )
        }
        cursor.close()
        return user
    }

    // Get Student details
    fun getStudent(admissionNo: String): Student? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT admission_no, first_name, last_name, gender, dob, phone, course_code, fee_balance, national_id, postal_address, next_of_kin_name, next_of_kin_relationship, next_of_kin_phone, enrollment_status, current_year, graduation_cleared FROM students WHERE admission_no = ?",
            arrayOf(admissionNo)
        )
        var student: Student? = null
        if (cursor.moveToFirst()) {
            student = Student(
                admissionNo = cursor.getString(0),
                firstName = cursor.getString(1),
                lastName = cursor.getString(2),
                gender = cursor.getString(3),
                dob = cursor.getString(4),
                phone = cursor.getString(5),
                courseCode = cursor.getString(6),
                feeBalance = cursor.getDouble(7),
                nationalId = cursor.getString(8) ?: "",
                postalAddress = cursor.getString(9) ?: "",
                nextOfKinName = cursor.getString(10) ?: "",
                nextOfKinRelationship = cursor.getString(11) ?: "",
                nextOfKinPhone = cursor.getString(12) ?: "",
                enrollmentStatus = cursor.getString(13) ?: "Active",
                currentYear = cursor.getInt(14),
                graduationCleared = cursor.getInt(15) == 1
            )
        }
        cursor.close()
        return student
    }

    // Get Lecturer details
    fun getLecturer(employeeNo: String): Lecturer? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT employee_no, first_name, last_name, department FROM lecturers WHERE employee_no = ?",
            arrayOf(employeeNo)
        )
        var lecturer: Lecturer? = null
        if (cursor.moveToFirst()) {
            lecturer = Lecturer(
                employeeNo = cursor.getString(0),
                firstName = cursor.getString(1),
                lastName = cursor.getString(2),
                department = cursor.getString(3)
            )
        }
        cursor.close()
        return lecturer
    }

    // Get Student's Grades list
    fun getStudentGrades(studentId: String): List<Grade> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT u.code, u.name, g.cat_mark, g.exam_mark 
            FROM enrollments e
            JOIN units u ON e.unit_code = u.code
            LEFT JOIN grades g ON e.id = g.enrollment_id
            WHERE e.student_id = ?
            """,
            arrayOf(studentId)
        )
        val grades = mutableListOf<Grade>()
        while (cursor.moveToNext()) {
            val cat = if (cursor.isNull(2)) null else cursor.getDouble(2)
            val exam = if (cursor.isNull(3)) null else cursor.getDouble(3)
            grades.add(
                Grade(
                    unitCode = cursor.getString(0),
                    unitName = cursor.getString(1),
                    catMark = cat,
                    examMark = exam
                )
            )
        }
        cursor.close()
        return grades
    }

    // Get Attendance Summary per unit for a student
    fun getStudentAttendanceSummary(studentId: String): List<AttendanceSummary> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT e.unit_code, u.name,
                   COUNT(a.id) as total_classes,
                   SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) as present_classes
            FROM enrollments e
            JOIN units u ON e.unit_code = u.code
            LEFT JOIN attendance a ON e.id = a.enrollment_id
            WHERE e.student_id = ?
            GROUP BY e.unit_code, u.name
            """,
            arrayOf(studentId)
        )
        val summaries = mutableListOf<AttendanceSummary>()
        while (cursor.moveToNext()) {
            summaries.add(
                AttendanceSummary(
                    unitCode = cursor.getString(0),
                    unitName = cursor.getString(1),
                    totalClasses = cursor.getInt(2),
                    presentCount = cursor.getInt(3)
                )
            )
        }
        cursor.close()
        return summaries
    }

    // Get Fee Statement and Balance
    fun getStudentFeeStatement(studentId: String): FeeStatement {
        val db = dbHelper.readableDatabase
        
        // Fetch student's fee balance to determine total invoiced vs paid
        val student = getStudent(studentId)
        val currentBalance = student?.feeBalance ?: 0.0

        val cursor = db.rawQuery(
            "SELECT id, amount, receipt_no, payment_date, description FROM fee_payments WHERE student_id = ? ORDER BY payment_date DESC",
            arrayOf(studentId)
        )
        val items = mutableListOf<FeeStatementItem>()
        var totalPaid = 0.0
        while (cursor.moveToNext()) {
            val amount = cursor.getDouble(1)
            totalPaid += amount
            items.add(
                FeeStatementItem(
                    id = cursor.getInt(0),
                    type = "PAYMENT",
                    amount = amount,
                    receiptNo = cursor.getString(2),
                    date = cursor.getString(3),
                    description = cursor.getString(4)
                )
            )
        }
        cursor.close()

        // Invoiced is mock total tuition invoice base
        val totalInvoiced = totalPaid + currentBalance

        // Add base tuition invoice item for visualization in statement ledger
        items.add(
            FeeStatementItem(
                id = -1,
                type = "INVOICE",
                amount = totalInvoiced,
                receiptNo = null,
                date = "2025-09-01",
                description = "Tuition Invoice - Year 1 Sem 2",
            )
        )

        // Sort items by date descending
        items.sortByDescending { it.date }

        return FeeStatement(
            items = items,
            totalInvoiced = totalInvoiced,
            totalPaid = totalPaid,
            balance = currentBalance
        )
    }

    // Get units allocated to a lecturer
    fun getLecturerUnits(employeeNo: String): List<UnitEntity> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT code, name, course_code FROM units WHERE lecturer_id = ?",
            arrayOf(employeeNo)
        )
        val units = mutableListOf<UnitEntity>()
        while (cursor.moveToNext()) {
            units.add(
                UnitEntity(
                    code = cursor.getString(0),
                    name = cursor.getString(1),
                    courseCode = cursor.getString(2)
                )
            )
        }
        cursor.close()
        return units
    }

    // Data class representing Unit entity for local query mapping
    data class UnitEntity(
        val code: String,
        val name: String,
        val courseCode: String
    )

    // Get list of students enrolled in a unit with grades/attendance summaries
    fun getStudentRoster(unitCode: String): List<StudentRosterItem> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT e.id as enrollment_id, s.admission_no, s.first_name || ' ' || s.last_name as student_name,
                   g.cat_mark, g.exam_mark,
                   COUNT(a.id) as total_classes,
                   SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) as present_count
            FROM enrollments e
            JOIN students s ON e.student_id = s.admission_no
            LEFT JOIN grades g ON e.id = g.enrollment_id
            LEFT JOIN attendance a ON e.id = a.enrollment_id
            WHERE e.unit_code = ?
            GROUP BY e.id, s.admission_no, student_name, g.cat_mark, g.exam_mark
            ORDER BY s.admission_no ASC
            """,
            arrayOf(unitCode)
        )
        val roster = mutableListOf<StudentRosterItem>()
        while (cursor.moveToNext()) {
            val cat = if (cursor.isNull(3)) null else cursor.getDouble(3)
            val exam = if (cursor.isNull(4)) null else cursor.getDouble(4)
            roster.add(
                StudentRosterItem(
                    enrollmentId = cursor.getInt(0),
                    studentId = cursor.getString(1),
                    studentName = cursor.getString(2),
                    catMark = cat,
                    examMark = exam,
                    totalClasses = cursor.getInt(5),
                    presentCount = cursor.getInt(6)
                )
            )
        }
        cursor.close()
        return roster
    }

    // Submit or update a student's grades
    fun submitGrades(enrollmentId: Int, cat: Double?, exam: Double?): Boolean {
        val db = dbHelper.writableDatabase
        
        // Check if grade record already exists
        val cursor = db.rawQuery("SELECT id FROM grades WHERE enrollment_id = ?", arrayOf(enrollmentId.toString()))
        val exists = cursor.moveToFirst()
        cursor.close()

        val values = ContentValues().apply {
            put("enrollment_id", enrollmentId)
            put("cat_mark", cat)
            put("exam_mark", exam)
        }

        return if (exists) {
            db.update("grades", values, "enrollment_id = ?", arrayOf(enrollmentId.toString())) > 0
        } else {
            db.insert("grades", null, values) > -1
        }
    }

    // Record attendance log for a student
    fun recordAttendance(enrollmentId: Int, date: String, status: String, recordedBy: String): Boolean {
        val db = dbHelper.writableDatabase

        // Check if attendance already recorded for this enrollment on this specific date
        val cursor = db.rawQuery(
            "SELECT id FROM attendance WHERE enrollment_id = ? AND date = ?",
            arrayOf(enrollmentId.toString(), date)
        )
        val exists = cursor.moveToFirst()
        cursor.close()

        val values = ContentValues().apply {
            put("enrollment_id", enrollmentId)
            put("date", date)
            put("status", status)
            put("recorded_by", recordedBy)
        }

        return if (exists) {
            db.update("attendance", values, "enrollment_id = ? AND date = ?", arrayOf(enrollmentId.toString(), date)) > 0
        } else {
            db.insert("attendance", null, values) > -1
        }
    }

    // Get all students
    fun getAllStudents(): List<Student> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT admission_no, first_name, last_name, gender, dob, phone, course_code, fee_balance, national_id, postal_address, next_of_kin_name, next_of_kin_relationship, next_of_kin_phone, enrollment_status, current_year, graduation_cleared FROM students ORDER BY admission_no ASC",
            null
        )
        val list = mutableListOf<Student>()
        while (cursor.moveToNext()) {
            list.add(Student(
                admissionNo = cursor.getString(0),
                firstName = cursor.getString(1),
                lastName = cursor.getString(2),
                gender = cursor.getString(3),
                dob = cursor.getString(4),
                phone = cursor.getString(5),
                courseCode = cursor.getString(6),
                feeBalance = cursor.getDouble(7),
                nationalId = cursor.getString(8) ?: "",
                postalAddress = cursor.getString(9) ?: "",
                nextOfKinName = cursor.getString(10) ?: "",
                nextOfKinRelationship = cursor.getString(11) ?: "",
                nextOfKinPhone = cursor.getString(12) ?: "",
                enrollmentStatus = cursor.getString(13) ?: "Active",
                currentYear = cursor.getInt(14),
                graduationCleared = cursor.getInt(15) == 1
            ))
        }
        cursor.close()
        return list
    }

    // Add or update student
    fun addOrUpdateStudent(student: Student): Boolean {
        val db = dbHelper.writableDatabase

        // Ensure user account exists
        val userValues = ContentValues().apply {
            put("username", student.admissionNo)
            put("password", "password123")
            put("role", "STUDENT")
            put("email", "${student.firstName.lowercase()}.${student.lastName.lowercase()}@kitalepoly.ac.ke")
        }
        db.insertWithOnConflict("users", null, userValues, SQLiteDatabase.CONFLICT_IGNORE)

        val userCursor = db.rawQuery("SELECT id FROM users WHERE username = ?", arrayOf(student.admissionNo))
        var userId = -1L
        if (userCursor.moveToFirst()) {
            userId = userCursor.getLong(0)
        }
        userCursor.close()

        val values = ContentValues().apply {
            put("admission_no", student.admissionNo)
            if (userId != -1L) put("user_id", userId)
            put("first_name", student.firstName)
            put("last_name", student.lastName)
            put("gender", student.gender)
            put("dob", student.dob)
            put("phone", student.phone)
            put("course_code", student.courseCode)
            put("fee_balance", student.feeBalance)
            put("national_id", student.nationalId)
            put("postal_address", student.postalAddress)
            put("next_of_kin_name", student.nextOfKinName)
            put("next_of_kin_relationship", student.nextOfKinRelationship)
            put("next_of_kin_phone", student.nextOfKinPhone)
            put("enrollment_status", student.enrollmentStatus)
            put("current_year", student.currentYear)
            put("graduation_cleared", if (student.graduationCleared) 1 else 0)
        }

        val existsCursor = db.rawQuery("SELECT admission_no FROM students WHERE admission_no = ?", arrayOf(student.admissionNo))
        val exists = existsCursor.moveToFirst()
        existsCursor.close()

        return if (exists) {
            db.update("students", values, "admission_no = ?", arrayOf(student.admissionNo)) > 0
        } else {
            db.insert("students", null, values) > -1
        }
    }

    // Get auxiliary student records
    fun getDisciplinaryRecords(studentId: String): List<DisciplinaryRecord> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT id, student_id, date, warning_level, description, status FROM disciplinary_records WHERE student_id = ? ORDER BY date DESC", arrayOf(studentId))
        val list = mutableListOf<DisciplinaryRecord>()
        while (cursor.moveToNext()) {
            list.add(DisciplinaryRecord(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5)))
        }
        cursor.close()
        return list
    }

    fun getExtracurricularActivities(studentId: String): List<ExtracurricularActivity> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT id, student_id, activity_name, role, year FROM extracurricular_activities WHERE student_id = ? ORDER BY year DESC", arrayOf(studentId))
        val list = mutableListOf<ExtracurricularActivity>()
        while (cursor.moveToNext()) {
            list.add(ExtracurricularActivity(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4)))
        }
        cursor.close()
        return list
    }

    fun getOfficialCommunications(studentId: String): List<OfficialCommunication> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT id, student_id, date, document_type, title, content FROM official_communications WHERE student_id = ? ORDER BY date DESC", arrayOf(studentId))
        val list = mutableListOf<OfficialCommunication>()
        while (cursor.moveToNext()) {
            list.add(OfficialCommunication(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5)))
        }
        cursor.close()
        return list
    }

    fun getPortfolioOfEvidences(studentId: String): List<PortfolioOfEvidence> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT id, student_id, unit_code, score, assessor_name, status FROM portfolios_of_evidence WHERE student_id = ? ORDER BY id DESC", arrayOf(studentId))
        val list = mutableListOf<PortfolioOfEvidence>()
        while (cursor.moveToNext()) {
            list.add(PortfolioOfEvidence(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getDouble(3), cursor.getString(4), cursor.getString(5)))
        }
        cursor.close()
        return list
    }

    fun getFinancialAidRecords(studentId: String): List<FinancialAid> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT id, student_id, amount, source, allocation_date FROM financial_aid WHERE student_id = ? ORDER BY allocation_date DESC", arrayOf(studentId))
        val list = mutableListOf<FinancialAid>()
        while (cursor.moveToNext()) {
            list.add(FinancialAid(cursor.getInt(0), cursor.getString(1), cursor.getDouble(2), cursor.getString(3), cursor.getString(4)))
        }
        cursor.close()
        return list
    }

    // Add entries manually
    fun addDisciplinaryRecord(studentId: String, warningLevel: String, description: String, date: String, status: String): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("student_id", studentId)
            put("date", date)
            put("warning_level", warningLevel)
            put("description", description)
            put("status", status)
        }
        return db.insert("disciplinary_records", null, values) > -1
    }

    fun addOfficialCommunication(studentId: String, documentType: String, title: String, content: String, date: String): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("student_id", studentId)
            put("date", date)
            put("document_type", documentType)
            put("title", title)
            put("content", content)
        }
        return db.insert("official_communications", null, values) > -1
    }

    fun addPortfolioOfEvidence(studentId: String, unitCode: String, score: Double, assessorName: String, status: String): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("student_id", studentId)
            put("unit_code", unitCode)
            put("score", score)
            put("assessor_name", assessorName)
            put("status", status)
        }
        return db.insert("portfolios_of_evidence", null, values) > -1
    }

    fun addFinancialAid(studentId: String, amount: Double, source: String, date: String): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("student_id", studentId)
            put("amount", amount)
            put("source", source)
            put("allocation_date", date)
        }
        return db.insert("financial_aid", null, values) > -1
    }

    // Import from CSV Stream
    fun importStudentsFromCsv(inputStream: java.io.InputStream): Int {
        val db = dbHelper.writableDatabase
        var importedCount = 0
        db.beginTransaction()
        try {
            val reader = inputStream.bufferedReader()
            val headerLine = reader.readLine() ?: return 0
            val headers = headerLine.split(",").map { it.trim().lowercase() }
            
            // Find column indices
            val admIdx = headers.indexOfFirst { it.contains("admission") || it.contains("adm") }
            val fnameIdx = headers.indexOfFirst { it.contains("first name") || it.contains("firstname") || it == "first_name" || it == "fname" }
            val lnameIdx = headers.indexOfFirst { it.contains("last name") || it.contains("lastname") || it == "last_name" || it == "lname" }
            val genderIdx = headers.indexOfFirst { it.contains("gender") || it == "sex" }
            val dobIdx = headers.indexOfFirst { it.contains("dob") || it.contains("birth") || it == "date_of_birth" }
            val phoneIdx = headers.indexOfFirst { it.contains("phone") || it == "tel" || it == "mobile" }
            val courseIdx = headers.indexOfFirst { it.contains("course") || it == "program" || it == "course_code" }
            val balanceIdx = headers.indexOfFirst { it.contains("balance") || it.contains("fee") || it == "fee_balance" }
            val nationalIdIdx = headers.indexOfFirst { it.contains("national id") || it.contains("national_id") || it == "nid" || it == "id" }
            val postalIdx = headers.indexOfFirst { it.contains("address") || it == "postal" || it == "postal_address" }
            val nokNameIdx = headers.indexOfFirst { it.contains("kin name") || it.contains("next of kin") || it == "nok_name" }
            val nokRelIdx = headers.indexOfFirst { it.contains("relationship") || it == "nok_relationship" || it == "rel" }
            val nokPhoneIdx = headers.indexOfFirst { it.contains("kin phone") || it == "nok_phone" }

            var line = reader.readLine()
            while (line != null) {
                val tokens = line.split(",")
                if (tokens.size > admIdx && admIdx != -1) {
                    val admissionNo = tokens[admIdx].trim()
                    if (admissionNo.isNotEmpty()) {
                        val firstName = if (fnameIdx != -1 && tokens.size > fnameIdx) tokens[fnameIdx].trim() else "Student"
                        val lastName = if (lnameIdx != -1 && tokens.size > lnameIdx) tokens[lnameIdx].trim() else ""
                        val gender = if (genderIdx != -1 && tokens.size > genderIdx) tokens[genderIdx].trim() else "M"
                        val dob = if (dobIdx != -1 && tokens.size > dobIdx) tokens[dobIdx].trim() else "2000-01-01"
                        val phone = if (phoneIdx != -1 && tokens.size > phoneIdx) tokens[phoneIdx].trim() else ""
                        val course = if (courseIdx != -1 && tokens.size > courseIdx) tokens[courseIdx].trim() else "DICT"
                        val balance = if (balanceIdx != -1 && tokens.size > balanceIdx) tokens[balanceIdx].trim().toDoubleOrNull() ?: 0.0 else 0.0
                        val nationalId = if (nationalIdIdx != -1 && tokens.size > nationalIdIdx) tokens[nationalIdIdx].trim() else ""
                        val postal = if (postalIdx != -1 && tokens.size > postalIdx) tokens[postalIdx].trim() else ""
                        val nokName = if (nokNameIdx != -1 && tokens.size > nokNameIdx) tokens[nokNameIdx].trim() else ""
                        val nokRel = if (nokRelIdx != -1 && tokens.size > nokRelIdx) tokens[nokRelIdx].trim() else ""
                        val nokPhone = if (nokPhoneIdx != -1 && tokens.size > nokPhoneIdx) tokens[nokPhoneIdx].trim() else ""

                        val userValues = ContentValues().apply {
                            put("username", admissionNo)
                            put("password", "password123")
                            put("role", "STUDENT")
                            put("email", "${firstName.lowercase()}.${lastName.lowercase()}@kitalepoly.ac.ke")
                        }
                        db.insertWithOnConflict("users", null, userValues, SQLiteDatabase.CONFLICT_IGNORE)

                        val userCursor = db.rawQuery("SELECT id FROM users WHERE username = ?", arrayOf(admissionNo))
                        var userId = -1L
                        if (userCursor.moveToFirst()) {
                            userId = userCursor.getLong(0)
                        }
                        userCursor.close()

                        val studentValues = ContentValues().apply {
                            put("admission_no", admissionNo)
                            if (userId != -1L) put("user_id", userId)
                            put("first_name", firstName)
                            put("last_name", lastName)
                            put("gender", gender)
                            put("dob", dob)
                            put("phone", phone)
                            put("course_code", course)
                            put("fee_balance", balance)
                            put("national_id", nationalId)
                            put("postal_address", postal)
                            put("next_of_kin_name", nokName)
                            put("next_of_kin_relationship", nokRel)
                            put("next_of_kin_phone", nokPhone)
                            put("enrollment_status", "Active")
                            put("current_year", 1)
                            put("graduation_cleared", 0)
                        }

                        val existsCursor = db.rawQuery("SELECT admission_no FROM students WHERE admission_no = ?", arrayOf(admissionNo))
                        val exists = existsCursor.moveToFirst()
                        existsCursor.close()

                        val success = if (exists) {
                            db.update("students", studentValues, "admission_no = ?", arrayOf(admissionNo)) > 0
                        } else {
                            db.insert("students", null, studentValues) > -1
                        }
                        if (success) {
                            importedCount++
                        }
                    }
                }
                line = reader.readLine()
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
        return importedCount
    }
}
