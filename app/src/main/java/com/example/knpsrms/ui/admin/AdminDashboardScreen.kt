package com.example.knpsrms.ui.admin

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.knpsrms.data.DataRepository
import com.example.knpsrms.data.models.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    adminId: String,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { DataRepository(context) }
    val viewModel: AdminViewModel = viewModel { AdminViewModel(repository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showAddStudentDialog by remember { mutableStateOf(false) }

    // CSV File Picker launcher
    val csvPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    viewModel.importCsv(inputStream)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadAllStudents()
    }

    // Monitor CSV Import Status and show toast
    LaunchedEffect(state) {
        if (state is AdminUiState.Success) {
            val successState = state as AdminUiState.Success
            successState.csvImportStatus?.let { status ->
                Toast.makeText(context, status, Toast.LENGTH_LONG).show()
                viewModel.clearImportStatus()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("KNP Admin Portal", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF5C1324)),
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = Color.White)
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val uiState = state) {
                is AdminUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is AdminUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(uiState.message, fontSize = 16.sp, color = Color.Red, textAlign = TextAlign.Center)
                    }
                }
                is AdminUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Quick Welcome & Tools header
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Logged In: Registrar", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("ID: $adminId", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Add Manual
                                    Button(
                                        onClick = { showAddStudentDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C1324)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Register", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    // Import CSV
                                    Button(
                                        onClick = { csvPickerLauncher.launch("text/*") },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("CSV Import", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Search box
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search by name or admission number...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        )

                        // Filter list
                        val filteredStudents = uiState.students.filter {
                            it.admissionNo.contains(searchQuery, ignoreCase = true) ||
                                    it.fullName.contains(searchQuery, ignoreCase = true)
                        }

                        if (filteredStudents.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                Text("No students found matching query.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(filteredStudents) { student ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.selectStudent(student) }
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(45.dp)
                                                    .background(Color(0xFFF5E6E8), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = student.firstName.take(1) + student.lastName.take(1),
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(student.fullName.uppercase(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text("ADM: ${student.admissionNo}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("Course: ${student.courseCode} | Year: ${student.currentYear}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                                            }
                                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Student Details Modal View
                    uiState.selectedStudent?.let { selectedStudent ->
                        StudentDetailDialog(
                            student = selectedStudent,
                            grades = uiState.grades,
                            feeStatement = uiState.feeStatement,
                            attendance = uiState.attendance,
                            disciplinary = uiState.disciplinaryRecords,
                            extracurricular = uiState.extracurricularActivities,
                            communications = uiState.officialCommunications,
                            portfolios = uiState.portfolios,
                            financialAid = uiState.financialAid,
                            onDismiss = { viewModel.clearSelectedStudent() },
                            onAddDisciplinary = { level, desc ->
                                viewModel.addDisciplinaryRecord(selectedStudent.admissionNo, level, desc, SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()), "Active")
                            },
                            onAddAid = { amount, src ->
                                viewModel.addFinancialAid(selectedStudent.admissionNo, amount, src, SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
                            },
                            onAddPoe = { code, score, assessor ->
                                viewModel.addPortfolioOfEvidence(selectedStudent.admissionNo, code, score, assessor, if (score >= 50) "Competent" else "NYC")
                            },
                            onAddComm = { docType, title, content ->
                                viewModel.addOfficialCommunication(selectedStudent.admissionNo, docType, title, content, SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
                            },
                            onUpdateGraduation = { cleared ->
                                val updated = selectedStudent.copy(graduationCleared = cleared)
                                viewModel.addOrUpdateStudent(updated)
                            }
                        )
                    }
                }
            }
        }

        // Add Student Dialog Form
        if (showAddStudentDialog) {
            AddStudentDialog(
                onDismiss = { showAddStudentDialog = false },
                onAddStudent = { student ->
                    viewModel.addOrUpdateStudent(student)
                    showAddStudentDialog = false
                    Toast.makeText(context, "Student enrolled successfully!", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentDetailDialog(
    student: Student,
    grades: List<Grade>,
    feeStatement: FeeStatement?,
    attendance: List<AttendanceSummary>,
    disciplinary: List<DisciplinaryRecord>,
    extracurricular: List<ExtracurricularActivity>,
    communications: List<OfficialCommunication>,
    portfolios: List<PortfolioOfEvidence>,
    financialAid: List<FinancialAid>,
    onDismiss: () -> Unit,
    onAddDisciplinary: (String, String) -> Unit,
    onAddAid: (Double, String) -> Unit,
    onAddPoe: (String, Double, String) -> Unit,
    onAddComm: (String, String, String) -> Unit,
    onUpdateGraduation: (Boolean) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Bio", "Academic", "Ledger", "Portfolio", "Actions")

    // Form inputs for subfolder manual additions
    var showActionForm by remember { mutableStateOf<String?>(null) } // "DISCIPLINARY", "FINAID", "POE", "COMM"

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        content = {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(student.fullName.uppercase(), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp) },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF5C1324)),
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                        }
                    )
                }
            ) { padding ->
                 Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.background)
                        .padding(16.dp)
                ) {
                    // Quick Stats strip
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Status", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(student.enrollmentStatus, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Grad Cleared", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(if (student.graduationCleared) "Yes" else "No", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Balance Due", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("KES ${String.format("%.0f", student.feeBalance)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            }
                        }
                    }

                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary, // Dark Maroon / Shiny Gold
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).padding(bottom = 12.dp)
                    ) {
                        tabs.forEachIndexed { index, title ->
                            val selected = selectedTab == index
                            Tab(
                                selected = selected,
                                onClick = { selectedTab = index; showActionForm = null },
                                selectedContentColor = MaterialTheme.colorScheme.primary,
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                text = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (selectedTab) {
                            0 -> // Bio folder
                                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    FolderSection("Demographic and Bio Data") {
                                        FolderItem("Admission No", student.admissionNo)
                                        FolderItem("Full Legal Name", student.fullName)
                                        FolderItem("Gender", if (student.gender == "M") "Male" else "Female")
                                        FolderItem("Date of Birth", student.dob)
                                        FolderItem("National ID", student.nationalId.ifEmpty { "Not Provided" })
                                    }
                                    FolderSection("Contact Information") {
                                        FolderItem("Phone", student.phone.ifEmpty { "Not Provided" })
                                        FolderItem("Institutional Email", "${student.firstName.lowercase()}.${student.lastName.lowercase()}@kitalepoly.ac.ke")
                                        FolderItem("Postal Address", student.postalAddress.ifEmpty { "Not Provided" })
                                    }
                                    FolderSection("Emergency Contacts") {
                                        FolderItem("Next of Kin Name", student.nextOfKinName.ifEmpty { "Not Provided" })
                                        FolderItem("Relationship", student.nextOfKinRelationship.ifEmpty { "Not Provided" })
                                        FolderItem("Contact Phone", student.nextOfKinPhone.ifEmpty { "Not Provided" })
                                    }
                                }
                            1 -> // Academic folder
                                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    FolderSection("Enrollment & Progression") {
                                        FolderItem("Current Program", student.courseCode)
                                        FolderItem("Current Year", "Year ${student.currentYear}")
                                        FolderItem("Enrollment Status", student.enrollmentStatus)
                                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Graduation Clearance Status:", fontSize = 12.sp, color = Color.Gray)
                                            Switch(checked = student.graduationCleared, onCheckedChange = { onUpdateGraduation(it) })
                                        }
                                    }
                                    FolderSection("Registered Competencies & Grades") {
                                        if (grades.isEmpty()) {
                                            Text("No academic grades logged.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 4.dp))
                                        } else {
                                            grades.forEach { grade ->
                                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("${grade.unitCode}: ${grade.unitName}", fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                                    Text("CAT: ${grade.catMark ?: "-"} | Exam: ${grade.examMark ?: "-"} | Grade: ${grade.gradeLetter}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5C1324))
                                                }
                                            }
                                        }
                                    }
                                    FolderSection("Attendance Summaries") {
                                        if (attendance.isEmpty()) {
                                            Text("No attendance summaries recorded.", fontSize = 12.sp, color = Color.Gray)
                                        } else {
                                            attendance.forEach { summary ->
                                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                        Text(summary.unitName, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                                        Text(String.format("%.1f%%", summary.percentage), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (summary.percentage >= 75) Color(0xFF2E7D32) else Color(0xFFC62828))
                                                    }
                                                    LinearProgressIndicator(
                                                        progress = { (summary.percentage / 100.0).toFloat() },
                                                        color = if (summary.percentage >= 75) Color(0xFF4CAF50) else Color(0xFFF44336),
                                                        trackColor = Color(0xFFE0E0E0),
                                                        modifier = Modifier.fillMaxWidth().height(4.dp).padding(top = 4.dp).clip(CircleShape)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            2 -> // Financial Ledger folder
                                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    FolderSection("Financial Aid Allocations") {
                                        if (financialAid.isEmpty()) {
                                            Text("No CDF/Government bursary aid recorded.", fontSize = 12.sp, color = Color.Gray)
                                        } else {
                                            financialAid.forEach { aid ->
                                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Column {
                                                        Text(aid.source, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                        Text("Date: ${aid.allocationDate}", fontSize = 10.sp, color = Color.Gray)
                                                    }
                                                    Text("KES ${String.format("%,.0f", aid.amount)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                                }
                                            }
                                        }
                                    }
                                    FolderSection("Transaction & Tuition ledger") {
                                        feeStatement?.items?.forEach { item ->
                                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(item.description, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    Text(if (item.type == "PAYMENT") "Receipt: ${item.receiptNo} | ${item.date}" else item.date, fontSize = 10.sp, color = Color.Gray)
                                                }
                                                Text(
                                                    text = if (item.type == "PAYMENT") "- KES ${String.format("%,.0f", item.amount)}" else "+ KES ${String.format("%,.0f", item.amount)}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (item.type == "PAYMENT") Color(0xFF2E7D32) else Color.Black
                                                )
                                            }
                                        }
                                    }
                                }
                            3 -> // Portfolios & Behavior
                                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    FolderSection("Competency Portfolios (PoE)") {
                                        if (portfolios.isEmpty()) {
                                            Text("No evidence portfolios uploaded.", fontSize = 12.sp, color = Color.Gray)
                                        } else {
                                            portfolios.forEach { poe ->
                                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Column {
                                                        Text("Competency Unit: ${poe.unitCode}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                        Text("Assessor: ${poe.assessorName} | Score: ${poe.score}%", fontSize = 10.sp, color = Color.Gray)
                                                    }
                                                    Text(poe.status, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (poe.status == "Competent") Color(0xFF2E7D32) else Color(0xFFFFA000))
                                                }
                                            }
                                        }
                                    }
                                    FolderSection("Disciplinary Records") {
                                        if (disciplinary.isEmpty()) {
                                            Text("No disciplinary records found.", fontSize = 12.sp, color = Color.Gray)
                                        } else {
                                            disciplinary.forEach { disc ->
                                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                        Text(disc.warningLevel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                                                        Text(disc.status, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                                    }
                                                    Text(disc.description, fontSize = 11.sp)
                                                    Text("Date Logged: ${disc.date}", fontSize = 9.sp, color = Color.Gray)
                                                }
                                            }
                                        }
                                    }
                                    FolderSection("Official Institutional Communications") {
                                        if (communications.isEmpty()) {
                                            Text("No memos or official clearances logged.", fontSize = 12.sp, color = Color.Gray)
                                        } else {
                                            communications.forEach { comm ->
                                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                                    Text("[${comm.documentType}] ${comm.title}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5C1324))
                                                    Text(comm.content, fontSize = 11.sp)
                                                    Text("Date: ${comm.date}", fontSize = 9.sp, color = Color.Gray)
                                                }
                                            }
                                        }
                                    }
                                }
                            4 -> // Actions folder
                                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(6.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("Add New Student Log Folder Entries", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { showActionForm = "DISCIPLINARY" }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red), modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                                            Text("Log Warning", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Button(onClick = { showActionForm = "FINAID" }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)), modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                                            Text("Add Aid/Bursary", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { showActionForm = "POE" }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)), modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                                            Text("Log Portfolio score", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Button(onClick = { showActionForm = "COMM" }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C1324)), modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                                            Text("Send Clearance Memo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Display selected Form
                                    showActionForm?.let { form ->
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        when (form) {
                                            "DISCIPLINARY" -> {
                                                var warning by remember { mutableStateOf("First Warning") }
                                                var desc by remember { mutableStateOf("") }
                                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    Text("Record Disciplinary warning", fontWeight = FontWeight.Bold)
                                                    OutlinedTextField(value = warning, onValueChange = { warning = it }, label = { Text("Warning Level") }, modifier = Modifier.fillMaxWidth())
                                                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Reason Description") }, modifier = Modifier.fillMaxWidth())
                                                    Button(
                                                        onClick = {
                                                            if (desc.isNotEmpty()) {
                                                                onAddDisciplinary(warning, desc)
                                                                showActionForm = null
                                                            }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text("Submit Record")
                                                    }
                                                }
                                            }
                                            "FINAID" -> {
                                                var amount by remember { mutableStateOf("") }
                                                var source by remember { mutableStateOf("CDF Bursary") }
                                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    Text("Allocate Financial Aid", fontWeight = FontWeight.Bold)
                                                    OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (KES)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                                                    OutlinedTextField(value = source, onValueChange = { source = it }, label = { Text("Source (e.g. CDF, HELB)") }, modifier = Modifier.fillMaxWidth())
                                                    Button(
                                                        onClick = {
                                                            val amt = amount.toDoubleOrNull()
                                                            if (amt != null && source.isNotEmpty()) {
                                                                onAddAid(amt, source)
                                                                showActionForm = null
                                                            }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text("Submit Allocation")
                                                    }
                                                }
                                            }
                                            "POE" -> {
                                                var code by remember { mutableStateOf("DICT-202") }
                                                var score by remember { mutableStateOf("") }
                                                var assessor by remember { mutableStateOf("Silas Wekesa") }
                                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    Text("Submit Evidence Portfolio score", fontWeight = FontWeight.Bold)
                                                    OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Unit Code") }, modifier = Modifier.fillMaxWidth())
                                                    OutlinedTextField(value = score, onValueChange = { score = it }, label = { Text("Assessment Score (%)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                                                    OutlinedTextField(value = assessor, onValueChange = { assessor = it }, label = { Text("Assessor Name") }, modifier = Modifier.fillMaxWidth())
                                                    Button(
                                                        onClick = {
                                                            val scr = score.toDoubleOrNull()
                                                            if (scr != null && code.isNotEmpty()) {
                                                                onAddPoe(code, scr, assessor)
                                                                showActionForm = null
                                                            }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text("Submit Portfolio Score")
                                                    }
                                                }
                                            }
                                            "COMM" -> {
                                                var type by remember { mutableStateOf("Clearance Certificate") }
                                                var title by remember { mutableStateOf("") }
                                                var content by remember { mutableStateOf("") }
                                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    Text("Log Official Communication Memo", fontWeight = FontWeight.Bold)
                                                    OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Memo Type") }, modifier = Modifier.fillMaxWidth())
                                                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                                                    OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Memo Contents") }, modifier = Modifier.fillMaxWidth())
                                                    Button(
                                                        onClick = {
                                                            if (title.isNotEmpty() && content.isNotEmpty()) {
                                                                onAddComm(type, title, content)
                                                                showActionForm = null
                                                            }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text("Issue Official Letter")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun FolderSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            content()
        }
    }
}

@Composable
private fun FolderItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddStudentDialog(
    onDismiss: () -> Unit,
    onAddStudent: (Student) -> Unit
) {
    var admissionNo by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("M") }
    var dob by remember { mutableStateOf("2005-01-01") }
    var phone by remember { mutableStateOf("") }
    var courseCode by remember { mutableStateOf("DICT") }
    var feeBalance by remember { mutableStateOf("0.0") }
    var nationalId by remember { mutableStateOf("") }
    var postalAddress by remember { mutableStateOf("") }
    var nokName by remember { mutableStateOf("") }
    var nokRel by remember { mutableStateOf("") }
    var nokPhone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enroll New Student", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(value = admissionNo, onValueChange = { admissionNo = it }, label = { Text("Admission Number") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text("First Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Last Name") }, modifier = Modifier.fillMaxWidth())
                
                // Gender Selection
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Gender: ")
                    RadioButton(selected = gender == "M", onClick = { gender = "M" })
                    Text("Male ")
                    RadioButton(selected = gender == "F", onClick = { gender = "F" })
                    Text("Female")
                }

                OutlinedTextField(value = dob, onValueChange = { dob = it }, label = { Text("Date of Birth (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = courseCode, onValueChange = { courseCode = it }, label = { Text("Course Code") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = feeBalance, onValueChange = { feeBalance = it }, label = { Text("Tuition Fee Balance (KES)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = nationalId, onValueChange = { nationalId = it }, label = { Text("National ID / Passport") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = postalAddress, onValueChange = { postalAddress = it }, label = { Text("Postal Address") }, modifier = Modifier.fillMaxWidth())
                
                Text("Emergency Contacts", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                OutlinedTextField(value = nokName, onValueChange = { nokName = it }, label = { Text("Next of Kin Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = nokRel, onValueChange = { nokRel = it }, label = { Text("Relationship") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = nokPhone, onValueChange = { nokPhone = it }, label = { Text("Kin Phone Number") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (admissionNo.isNotEmpty() && firstName.isNotEmpty() && lastName.isNotEmpty()) {
                        val student = Student(
                            admissionNo = admissionNo,
                            firstName = firstName,
                            lastName = lastName,
                            gender = gender,
                            dob = dob,
                            phone = phone,
                            courseCode = courseCode,
                            feeBalance = feeBalance.toDoubleOrNull() ?: 0.0,
                            nationalId = nationalId,
                            postalAddress = postalAddress,
                            nextOfKinName = nokName,
                            nextOfKinRelationship = nokRel,
                            nextOfKinPhone = nokPhone,
                            enrollmentStatus = "Active",
                            currentYear = 1,
                            graduationCleared = false
                        )
                        onAddStudent(student)
                    }
                }
            ) {
                Text("Enroll Student")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
