package com.example.knpsrms.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class SrmsDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "knp_srms.db"
        const val DATABASE_VERSION = 2

        // Tables Names
        const val TABLE_USERS = "users"
        const val TABLE_STUDENTS = "students"
        const val TABLE_LECTURERS = "lecturers"
        const val TABLE_UNITS = "units"
        const val TABLE_ENROLLMENTS = "enrollments"
        const val TABLE_GRADES = "grades"
        const val TABLE_ATTENDANCE = "attendance"
        const val TABLE_FEE_PAYMENTS = "fee_payments"
        
        const val TABLE_DISCIPLINARY = "disciplinary_records"
        const val TABLE_EXTRACURRICULAR = "extracurricular_activities"
        const val TABLE_COMMUNICATIONS = "official_communications"
        const val TABLE_POE = "portfolios_of_evidence"
        const val TABLE_FINANCIAL_AID = "financial_aid"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create tables
        db.execSQL("""
            CREATE TABLE $TABLE_USERS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE,
                password TEXT,
                role TEXT,
                email TEXT
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_STUDENTS (
                admission_no TEXT PRIMARY KEY,
                user_id INTEGER,
                first_name TEXT,
                last_name TEXT,
                gender TEXT,
                dob TEXT,
                phone TEXT,
                course_code TEXT,
                fee_balance REAL,
                national_id TEXT,
                postal_address TEXT,
                next_of_kin_name TEXT,
                next_of_kin_relationship TEXT,
                next_of_kin_phone TEXT,
                enrollment_status TEXT,
                current_year INTEGER,
                graduation_cleared INTEGER,
                FOREIGN KEY(user_id) REFERENCES $TABLE_USERS(id)
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_LECTURERS (
                employee_no TEXT PRIMARY KEY,
                user_id INTEGER,
                first_name TEXT,
                last_name TEXT,
                department TEXT,
                FOREIGN KEY(user_id) REFERENCES $TABLE_USERS(id)
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_UNITS (
                code TEXT PRIMARY KEY,
                name TEXT,
                course_code TEXT,
                lecturer_id TEXT,
                FOREIGN KEY(lecturer_id) REFERENCES $TABLE_LECTURERS(employee_no)
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_ENROLLMENTS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                student_id TEXT,
                unit_code TEXT,
                semester TEXT,
                academic_year TEXT,
                FOREIGN KEY(student_id) REFERENCES $TABLE_STUDENTS(admission_no),
                FOREIGN KEY(unit_code) REFERENCES $TABLE_UNITS(code)
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_GRADES (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                enrollment_id INTEGER,
                cat_mark REAL,
                exam_mark REAL,
                FOREIGN KEY(enrollment_id) REFERENCES $TABLE_ENROLLMENTS(id)
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_ATTENDANCE (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                enrollment_id INTEGER,
                date TEXT,
                status TEXT,
                recorded_by TEXT,
                FOREIGN KEY(enrollment_id) REFERENCES $TABLE_ENROLLMENTS(id),
                FOREIGN KEY(recorded_by) REFERENCES $TABLE_LECTURERS(employee_no)
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_FEE_PAYMENTS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                student_id TEXT,
                amount REAL,
                receipt_no TEXT,
                payment_date TEXT,
                description TEXT,
                FOREIGN KEY(student_id) REFERENCES $TABLE_STUDENTS(admission_no)
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_DISCIPLINARY (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                student_id TEXT,
                date TEXT,
                warning_level TEXT,
                description TEXT,
                status TEXT,
                FOREIGN KEY(student_id) REFERENCES $TABLE_STUDENTS(admission_no)
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_EXTRACURRICULAR (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                student_id TEXT,
                activity_name TEXT,
                role TEXT,
                year TEXT,
                FOREIGN KEY(student_id) REFERENCES $TABLE_STUDENTS(admission_no)
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_COMMUNICATIONS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                student_id TEXT,
                date TEXT,
                document_type TEXT,
                title TEXT,
                content TEXT,
                FOREIGN KEY(student_id) REFERENCES $TABLE_STUDENTS(admission_no)
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_POE (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                student_id TEXT,
                unit_code TEXT,
                score REAL,
                assessor_name TEXT,
                status TEXT,
                FOREIGN KEY(student_id) REFERENCES $TABLE_STUDENTS(admission_no),
                FOREIGN KEY(unit_code) REFERENCES $TABLE_UNITS(code)
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_FINANCIAL_AID (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                student_id TEXT,
                amount REAL,
                source TEXT,
                allocation_date TEXT,
                FOREIGN KEY(student_id) REFERENCES $TABLE_STUDENTS(admission_no)
            )
        """)

        // Pre-populate mock data
        insertMockData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FINANCIAL_AID")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_POE")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_COMMUNICATIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EXTRACURRICULAR")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DISCIPLINARY")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FEE_PAYMENTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ATTENDANCE")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_GRADES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ENROLLMENTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_UNITS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LECTURERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_STUDENTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    private fun insertMockData(db: SQLiteDatabase) {
        // 1. Insert Users
        // Student User
        val sUser = ContentValues().apply {
            put("username", "KNP/ICT/2024/099")
            put("password", "password123")
            put("role", "STUDENT")
            put("email", "john.doe@kitalepoly.ac.ke")
        }
        val sUserId = db.insert(TABLE_USERS, null, sUser)

        // Lecturer User
        val lUser = ContentValues().apply {
            put("username", "KNP/LEC/402")
            put("password", "password123")
            put("role", "LECTURER")
            put("email", "silas.wekesa@kitalepoly.ac.ke")
        }
        val lUserId = db.insert(TABLE_USERS, null, lUser)

        // Admin User
        val aUser = ContentValues().apply {
            put("username", "KNP/ADM/001")
            put("password", "password123")
            put("role", "ADMIN")
            put("email", "admin@kitalepoly.ac.ke")
        }
        db.insert(TABLE_USERS, null, aUser)

        // 2. Insert Student info
        val student = ContentValues().apply {
            put("admission_no", "KNP/ICT/2024/099")
            put("user_id", sUserId)
            put("first_name", "John")
            put("last_name", "Doe Omwamba")
            put("gender", "M")
            put("dob", "2004-05-12")
            put("phone", "0712345678")
            put("course_code", "DICT")
            put("fee_balance", 12400.00)
            put("national_id", "40123456")
            put("postal_address", "P.O. Box 123-30200, Kitale")
            put("next_of_kin_name", "Sarah Omwamba")
            put("next_of_kin_relationship", "Mother")
            put("next_of_kin_phone", "0722998877")
            put("enrollment_status", "Active")
            put("current_year", 1)
            put("graduation_cleared", 0)
        }
        db.insert(TABLE_STUDENTS, null, student)

        // Additional dummy students for lecturer roster
        val studentsList = listOf(
            Triple("KNP/ICT/2024/001", "Alice", "Auma"),
            Triple("KNP/ICT/2024/004", "Brian", "Kiprop"),
            Triple("KNP/ICT/2024/011", "Caleb", "Wanjala"),
            Triple("KNP/ICT/2024/020", "Diana", "Chebet")
        )
        for ((adm, fname, lname) in studentsList) {
            val uId = db.insert(TABLE_USERS, null, ContentValues().apply {
                put("username", adm)
                put("password", "password123")
                put("role", "STUDENT")
                put("email", "${fname.lowercase()}.${lname.lowercase()}@kitalepoly.ac.ke")
            })
            db.insert(TABLE_STUDENTS, null, ContentValues().apply {
                put("admission_no", adm)
                put("user_id", uId)
                put("first_name", fname)
                put("last_name", lname)
                put("gender", if (fname == "Alice" || fname == "Diana") "F" else "M")
                put("dob", "2005-01-01")
                put("phone", "0700000000")
                put("course_code", "DICT")
                put("fee_balance", 5000.00)
                put("national_id", "4000000" + adm.takeLast(1))
                put("postal_address", "P.O. Box 456, Kitale")
                put("next_of_kin_name", "Parent of " + fname)
                put("next_of_kin_relationship", "Father")
                put("next_of_kin_phone", "0711111111")
                put("enrollment_status", "Active")
                put("current_year", 1)
                put("graduation_cleared", 0)
            })
        }

        // 3. Insert Lecturer info
        val lecturer = ContentValues().apply {
            put("employee_no", "KNP/LEC/402")
            put("user_id", lUserId)
            put("first_name", "Silas")
            put("last_name", "Wekesa")
            put("department", "ICT")
        }
        db.insert(TABLE_LECTURERS, null, lecturer)

        // 4. Insert Units
        val units = listOf(
            Triple("DICT-201", "Database Management Systems", "DICT"),
            Triple("DICT-202", "Mobile Application Development", "DICT"),
            Triple("DICT-203", "System Analysis and Design", "DICT")
        )
        for ((code, name, course) in units) {
            val unit = ContentValues().apply {
                put("code", code)
                put("name", name)
                put("course_code", course)
                put("lecturer_id", "KNP/LEC/402")
            }
            db.insert(TABLE_UNITS, null, unit)
        }

        // 5. Enrollments & Grades
        val allStudents = listOf("KNP/ICT/2024/099") + studentsList.map { it.first }
        
        for (stdId in allStudents) {
            for ((code, _, _) in units) {
                val enrollment = ContentValues().apply {
                    put("student_id", stdId)
                    put("unit_code", code)
                    put("semester", "YEAR 1 SEM 2")
                    put("academic_year", "2025/2026")
                }
                val enrollId = db.insert(TABLE_ENROLLMENTS, null, enrollment)

                // Grades for primary test student John Doe
                if (stdId == "KNP/ICT/2024/099") {
                    val grade = ContentValues().apply {
                        put("enrollment_id", enrollId)
                        when (code) {
                            "DICT-201" -> {
                                put("cat_mark", 22.0)
                                put("exam_mark", 48.0)
                            }
                            "DICT-202" -> {
                                put("cat_mark", 18.5)
                                put("exam_mark", 50.0)
                            }
                            "DICT-203" -> {
                                put("cat_mark", 26.0)
                                put("exam_mark", 62.0)
                            }
                        }
                    }
                    db.insert(TABLE_GRADES, null, grade)
                }

                // Insert some mock attendance records for the student
                val dates = listOf("2026-05-18", "2026-05-20", "2026-05-25", "2026-05-27", "2026-06-01", "2026-06-03")
                for ((idx, date) in dates.withIndex()) {
                    val attendance = ContentValues().apply {
                        put("enrollment_id", enrollId)
                        put("date", date)
                        // Make John present in 5/6 classes (83%) and others random
                        val isPresent = if (stdId == "KNP/ICT/2024/099") {
                            idx != 2 // Absent on 3rd date
                        } else {
                            (idx % 4) != 0 // Some absences for others
                        }
                        put("status", if (isPresent) "PRESENT" else "ABSENT")
                        put("recorded_by", "KNP/LEC/402")
                    }
                    db.insert(TABLE_ATTENDANCE, null, attendance)
                }
            }
        }

        // 6. Fee payments & details
        val payments = listOf(
            Triple(15000.00, "REC-49280", "2026-01-10"),
            Triple(10000.00, "REC-50122", "2026-05-02")
        )
        for ((amount, receipt, date) in payments) {
            val payment = ContentValues().apply {
                put("student_id", "KNP/ICT/2024/099")
                put("amount", amount)
                put("receipt_no", receipt)
                put("payment_date", date)
                put("description", "Tuition Fee Installment Payment")
            }
            db.insert(TABLE_FEE_PAYMENTS, null, payment)
        }

        // 7. Insert Expanded Records for John Doe
        // Disciplinary Record
        val disc = ContentValues().apply {
            put("student_id", "KNP/ICT/2024/099")
            put("date", "2026-03-10")
            put("warning_level", "First Warning")
            put("description", "Noise making in the library during revision hours.")
            put("status", "Resolved")
        }
        db.insert(TABLE_DISCIPLINARY, null, disc)

        // Extracurricular Activities
        val extra = ContentValues().apply {
            put("student_id", "KNP/ICT/2024/099")
            put("activity_name", "KNP Rugby Club")
            put("role", "Team Captain")
            put("year", "2025/2026")
        }
        db.insert(TABLE_EXTRACURRICULAR, null, extra)

        // Official Communications
        val comm = ContentValues().apply {
            put("student_id", "KNP/ICT/2024/099")
            put("date", "2026-04-15")
            put("document_type", "Clearance Certificate")
            put("title", "Inter-Semester Sport Clearance")
            put("content", "This is to certify that John Doe Omwamba has returned all sports kits and is cleared for tournament participation.")
        }
        db.insert(TABLE_COMMUNICATIONS, null, comm)

        // Portfolio of Competency (PoE)
        val poe = ContentValues().apply {
            put("student_id", "KNP/ICT/2024/099")
            put("unit_code", "DICT-202")
            put("score", 85.5)
            put("assessor_name", "Silas Wekesa")
            put("status", "Competent")
        }
        db.insert(TABLE_POE, null, poe)

        // Financial Aid
        val aid = ContentValues().apply {
            put("student_id", "KNP/ICT/2024/099")
            put("amount", 5000.00)
            put("source", "CDF Bursary - Kiminini Constituency")
            put("allocation_date", "2026-02-14")
        }
        db.insert(TABLE_FINANCIAL_AID, null, aid)
    }
}
