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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.knpsrms.data.DataRepository
import com.example.knpsrms.data.models.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Custom Theme Colors matching KNP Brand Guide
private val DarkMaroon = Color(0xFF5C1324)
private val ShinyGold = Color(0xFFD4AF37)
private val DoveGray = Color(0xFF8D99AE)
private val LightDoveGray = Color(0xFFECEEF1)

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
    val departments by viewModel.departments.collectAsStateWithLifecycle()
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val units by viewModel.units.collectAsStateWithLifecycle()
    val userProfiles by viewModel.userProfiles.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0: Students, 1: Curriculum, 2: Users, 3: Logs
    val mainTabs = listOf("Students", "Curriculum", "Users", "System Logs")

    var searchQuery by remember { mutableStateOf("") }
    var showAddStudentDialog by remember { mutableStateOf(false) }

    // State for curriculum creation dialogs
    var showAddDeptDialog by remember { mutableStateOf(false) }
    var showAddCourseDialog by remember { mutableStateOf(false) }
    var showAddUnitDialog by remember { mutableStateOf(false) }
    var showAddUserDialog by remember { mutableStateOf(false) }

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
        viewModel.loadCurriculum()
        viewModel.loadUsers()
        viewModel.loadAuditLogs()
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
                title = { Text("KNP Registrar Portal", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkMaroon),
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = Color.White)
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Top Tab Row
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).padding(bottom = 8.dp)
            ) {
                mainTabs.forEachIndexed { index, title ->
                    val selected = activeTab == index
                    Tab(
                        selected = selected,
                        onClick = { activeTab = index },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        text = {
                            Text(
                                text = title,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (activeTab) {
                    0 -> { // Students Tab
                        when (val uiState = state) {
                            is AdminUiState.Loading -> {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = DarkMaroon)
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
                                                Text("Logged In: Registrar", fontSize = 11.sp, color = DoveGray)
                                                Text("ID: $adminId", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkMaroon)
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = { showAddStudentDialog = true },
                                                    colors = ButtonDefaults.buttonColors(containerColor = DarkMaroon),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Register", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Button(
                                                    onClick = { csvPickerLauncher.launch("text/*") },
                                                    colors = ButtonDefaults.buttonColors(containerColor = ShinyGold, contentColor = Color.Black),
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
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = DarkMaroon,
                                            focusedLabelColor = DarkMaroon
                                        ),
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
                                            Text("No students found matching query.", color = DoveGray)
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
                                                                color = DarkMaroon
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(14.dp))
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(student.fullName.uppercase(), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkMaroon)
                                                            Text("ADM: ${student.admissionNo}", fontSize = 12.sp, color = Color.Black)
                                                            Text("Course: ${student.courseCode} | Year: ${student.currentYear}", fontSize = 11.sp, color = DoveGray, fontWeight = FontWeight.SemiBold)
                                                        }
                                                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = DoveGray)
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
                                        availableUnits = units,
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
                                        },
                                        onEnrollUnit = { unitCode ->
                                            viewModel.enrollStudentInUnit(selectedStudent.admissionNo, unitCode)
                                        },
                                        onUpdateGrades = { unitCode, cat, exam ->
                                            viewModel.updateStudentGrade(selectedStudent.admissionNo, unitCode, cat, exam)
                                        },
                                        onRecordAttendance = { unitCode, date, status ->
                                            viewModel.recordAttendance(selectedStudent.admissionNo, unitCode, date, status)
                                        },
                                        onRecordPayment = { amount, receipt, date, desc, callback ->
                                            viewModel.recordFeePayment(selectedStudent.admissionNo, amount, receipt, date, desc, callback)
                                        },
                                        onRecordInvoice = { amount, date, desc ->
                                            viewModel.recordTuitionInvoice(selectedStudent.admissionNo, amount, date, desc)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    1 -> { // Curriculum Setup Tab
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Header Actions Card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Curriculum Setup Modules", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkMaroon)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { showAddDeptDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = DarkMaroon),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("+ Dept", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { showAddCourseDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = ShinyGold, contentColor = Color.Black),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("+ Course", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { showAddUnitDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = DoveGray),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("+ Unit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // Departments List
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Academic Departments", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkMaroon)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (departments.isEmpty()) {
                                        Text("No departments configured.", fontSize = 12.sp, color = DoveGray)
                                    } else {
                                        departments.forEach { dept ->
                                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                    Text(dept.code, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DarkMaroon)
                                                    Text(dept.name, fontSize = 12.sp)
                                                }
                                                HorizontalDivider(color = LightDoveGray.copy(alpha = 0.5f), modifier = Modifier.padding(top = 4.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            // Courses List
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Configured Courses", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkMaroon)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (courses.isEmpty()) {
                                        Text("No courses configured.", fontSize = 12.sp, color = DoveGray)
                                    } else {
                                        courses.forEach { course ->
                                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                    Text(course.code, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DarkMaroon)
                                                    Text(course.name, fontSize = 12.sp, modifier = Modifier.weight(1f).padding(horizontal = 8.dp), textAlign = TextAlign.End)
                                                    Text(course.departmentCode, fontSize = 11.sp, color = DoveGray, fontWeight = FontWeight.SemiBold)
                                                }
                                                HorizontalDivider(color = LightDoveGray.copy(alpha = 0.5f), modifier = Modifier.padding(top = 4.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            // Units List
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Competency Units Setup", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkMaroon)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (units.isEmpty()) {
                                        Text("No units configured.", fontSize = 12.sp, color = DoveGray)
                                    } else {
                                        units.forEach { unit ->
                                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                    Text(unit.code, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DarkMaroon)
                                                    Text(unit.name, fontSize = 12.sp, modifier = Modifier.weight(1f).padding(horizontal = 8.dp), textAlign = TextAlign.End)
                                                    Text("Course: ${unit.courseCode}", fontSize = 11.sp, color = DoveGray, fontWeight = FontWeight.SemiBold)
                                                }
                                                HorizontalDivider(color = LightDoveGray.copy(alpha = 0.5f), modifier = Modifier.padding(top = 4.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> { // User Accounts Tab
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Credentials Profiles", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkMaroon)
                                    Button(
                                        onClick = { showAddUserDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = DarkMaroon),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Provision User", fontSize = 11.sp)
                                    }
                                }
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().weight(1f)
                            ) {
                                LazyColumn(
                                    contentPadding = PaddingValues(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(userProfiles) { user ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(
                                                        color = if (user.role == "ADMIN") Color(0xFFFDF2F4) else Color(0xFFE8F5E9),
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (user.role == "ADMIN") Icons.Default.AdminPanelSettings else Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = if (user.role == "ADMIN") DarkMaroon else Color(0xFF2E7D32)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(user.username, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(user.email, fontSize = 11.sp, color = DoveGray)
                                            }
                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (user.role == "ADMIN") Color(0xFFFDF2F4) else Color(0xFFE8F5E9)
                                                ),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = user.role,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (user.role == "ADMIN") DarkMaroon else Color(0xFF2E7D32),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            // Enable deletion if not the main admin user to prevent locking out
                                            val isLocked = user.username == "KNP/ADM/001" || user.username == adminId
                                            IconButton(
                                                onClick = { viewModel.deleteUser(user.id) },
                                                enabled = !isLocked
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete account",
                                                    tint = if (isLocked) DoveGray else Color.Red
                                                )
                                            }
                                        }
                                        HorizontalDivider(color = LightDoveGray)
                                    }
                                }
                            }
                        }
                    }

                    3 -> { // System Logs Tab
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("System Audit Trail Logs", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkMaroon)
                                        IconButton(onClick = { viewModel.loadAuditLogs() }) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Refresh logs", tint = DarkMaroon)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    if (auditLogs.isEmpty()) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("No audit logs available.", color = DoveGray)
                                        }
                                    } else {
                                        LazyColumn(
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(auditLogs) { log ->
                                                Column(modifier = Modifier.fillMaxWidth()) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Card(
                                                            colors = CardDefaults.cardColors(
                                                                containerColor = when (log.actionType) {
                                                                    "FINANCIAL" -> Color(0xFFE8F5E9)
                                                                    "ACADEMIC" -> Color(0xFFFFF3E0)
                                                                    "USERS" -> Color(0xFFE8EAF6)
                                                                    else -> Color(0xFFF5F5F5)
                                                                }
                                                            ),
                                                            shape = RoundedCornerShape(6.dp)
                                                        ) {
                                                            Text(
                                                                text = log.actionType,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = when (log.actionType) {
                                                                    "FINANCIAL" -> Color(0xFF2E7D32)
                                                                    "ACADEMIC" -> Color(0xFFE65100)
                                                                    "USERS" -> Color(0xFF3F51B5)
                                                                    else -> Color.DarkGray
                                                                },
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                        Text(log.timestamp, fontSize = 10.sp, color = DoveGray)
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(log.description, fontSize = 12.sp, color = Color.Black)
                                                    HorizontalDivider(color = LightDoveGray, modifier = Modifier.padding(top = 8.dp))
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

        // Add Student Dialog Form
        if (showAddStudentDialog) {
            AddStudentDialog(
                coursesList = courses,
                onDismiss = { showAddStudentDialog = false },
                onAddStudent = { student ->
                    viewModel.addOrUpdateStudent(student)
                    showAddStudentDialog = false
                    Toast.makeText(context, "Student enrolled successfully!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Add Department Dialog
        if (showAddDeptDialog) {
            var code by remember { mutableStateOf("") }
            var name by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showAddDeptDialog = false },
                title = { Text("Add Academic Department", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Department Code (e.g. ICT)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Department Name") }, modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (code.isNotEmpty() && name.isNotEmpty()) {
                                viewModel.addDepartment(code.uppercase().trim(), name.trim())
                                showAddDeptDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkMaroon)
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDeptDialog = false }) { Text("Cancel") }
                }
            )
        }

        // Add Course Dialog
        if (showAddCourseDialog) {
            var code by remember { mutableStateOf("") }
            var name by remember { mutableStateOf("") }
            var selectedDept by remember { mutableStateOf(departments.firstOrNull()?.code ?: "ICT") }
            var deptExpanded by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showAddCourseDialog = false },
                title = { Text("Add Course Program", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Course Code (e.g. DICT)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Course Name") }, modifier = Modifier.fillMaxWidth())
                        
                        Text("Department:", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { deptExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(selectedDept, color = DarkMaroon)
                            }
                            DropdownMenu(expanded = deptExpanded, onDismissRequest = { deptExpanded = false }) {
                                departments.forEach { dept ->
                                    DropdownMenuItem(
                                        text = { Text(dept.code + " - " + dept.name) },
                                        onClick = {
                                            selectedDept = dept.code
                                            deptExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (code.isNotEmpty() && name.isNotEmpty()) {
                                viewModel.addCourse(code.uppercase().trim(), name.trim(), selectedDept)
                                showAddCourseDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkMaroon)
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddCourseDialog = false }) { Text("Cancel") }
                }
            )
        }

        // Add Unit Dialog
        if (showAddUnitDialog) {
            var code by remember { mutableStateOf("") }
            var name by remember { mutableStateOf("") }
            var selectedCourse by remember { mutableStateOf(courses.firstOrNull()?.code ?: "DICT") }
            var courseExpanded by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showAddUnitDialog = false },
                title = { Text("Configure Unit Competency", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Unit Code (e.g. DICT-204)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Unit Name") }, modifier = Modifier.fillMaxWidth())
                        
                        Text("Course:", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { courseExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(selectedCourse, color = DarkMaroon)
                            }
                            DropdownMenu(expanded = courseExpanded, onDismissRequest = { courseExpanded = false }) {
                                courses.forEach { crs ->
                                    DropdownMenuItem(
                                        text = { Text(crs.code + " - " + crs.name) },
                                        onClick = {
                                            selectedCourse = crs.code
                                            courseExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (code.isNotEmpty() && name.isNotEmpty()) {
                                viewModel.addUnit(code.uppercase().trim(), name.trim(), selectedCourse, null)
                                showAddUnitDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkMaroon)
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddUnitDialog = false }) { Text("Cancel") }
                }
            )
        }

        // Add User Account Dialog
        if (showAddUserDialog) {
            var username by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            var email by remember { mutableStateOf("") }
            var role by remember { mutableStateOf("STUDENT") }
            var roleExpanded by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showAddUserDialog = false },
                title = { Text("Provision User Credentials", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username (Admission / ID)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Initial Password") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Contact Email") }, modifier = Modifier.fillMaxWidth())
                        
                        Text("Role:", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { roleExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(role, color = DarkMaroon)
                            }
                            DropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }) {
                                listOf("STUDENT", "ADMIN").forEach { r ->
                                    DropdownMenuItem(
                                        text = { Text(r) },
                                        onClick = {
                                            role = r
                                            roleExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (username.isNotEmpty() && password.isNotEmpty()) {
                                viewModel.addUser(username.trim(), password.trim(), role, email.trim())
                                showAddUserDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkMaroon)
                    ) {
                        Text("Provision")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddUserDialog = false }) { Text("Cancel") }
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
    availableUnits: List<CourseUnit>,
    onDismiss: () -> Unit,
    onAddDisciplinary: (String, String) -> Unit,
    onAddAid: (Double, String) -> Unit,
    onAddPoe: (String, Double, String) -> Unit,
    onAddComm: (String, String, String) -> Unit,
    onUpdateGraduation: (Boolean) -> Unit,
    onEnrollUnit: (String) -> Unit,
    onUpdateGrades: (String, Double?, Double?) -> Unit,
    onRecordAttendance: (String, String, String) -> Unit,
    onRecordPayment: (Double, String, String, String, (Boolean) -> Unit) -> Unit,
    onRecordInvoice: (Double, String, String) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Bio", "Academic", "Ledger", "Portfolio", "Actions")

    // Form inputs for subfolder manual additions
    var showActionForm by remember { mutableStateOf<String?>(null) } // "DISCIPLINARY", "FINAID", "POE", "COMM"

    // Dialog state variables for direct registrar edits
    var showEnrollDialog by remember { mutableStateOf(false) }
    var showAttendanceDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showInvoiceDialog by remember { mutableStateOf(false) }
    
    var showEditMarksUnitCode by remember { mutableStateOf<String?>(null) }

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
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkMaroon),
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
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Status", fontSize = 10.sp, color = DoveGray)
                                Text(student.enrollmentStatus, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkMaroon)
                            }
                        }
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Grad Cleared", fontSize = 10.sp, color = DoveGray)
                                Text(if (student.graduationCleared) "Yes" else "No", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ShinyGold)
                            }
                        }
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Balance Due", fontSize = 10.sp, color = DoveGray)
                                Text("KES ${String.format("%.0f", student.feeBalance)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (student.feeBalance > 0.0) Color(0xFFC62828) else Color(0xFF2E7D32))
                            }
                        }
                    }

                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = DarkMaroon,
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
                                            Switch(
                                                checked = student.graduationCleared,
                                                onCheckedChange = { onUpdateGraduation(it) },
                                                colors = SwitchDefaults.colors(checkedThumbColor = DarkMaroon, checkedTrackColor = ShinyGold)
                                            )
                                        }
                                    }

                                    // Registrar Direct Actions Area
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text("Registrar Academic Actions", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkMaroon)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = { showEnrollDialog = true },
                                                    colors = ButtonDefaults.buttonColors(containerColor = DarkMaroon),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Enroll Unit", fontSize = 11.sp)
                                                }
                                                Button(
                                                    onClick = { showAttendanceDialog = true },
                                                    colors = ButtonDefaults.buttonColors(containerColor = ShinyGold, contentColor = Color.Black),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Log Attendance", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }

                                    FolderSection("Registered Competencies & Grades") {
                                        if (grades.isEmpty()) {
                                            Text("No academic grades logged. Please enroll student in units.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 4.dp))
                                        } else {
                                            grades.forEach { grade ->
                                                Column(modifier = Modifier.fillMaxWidth()) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text("${grade.unitCode}: ${grade.unitName}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                                            Text(
                                                                text = "CAT: ${grade.catMark ?: "-"} | Exam: ${grade.examMark ?: "-"} | Grade: ${grade.gradeLetter}",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = DarkMaroon
                                                            )
                                                        }
                                                        IconButton(onClick = { showEditMarksUnitCode = grade.unitCode }) {
                                                            Icon(Icons.Default.Edit, contentDescription = "Edit grades", tint = ShinyGold, modifier = Modifier.size(18.dp))
                                                        }
                                                    }
                                                    HorizontalDivider(color = LightDoveGray.copy(alpha = 0.5f))
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
                                                        trackColor = LightDoveGray,
                                                        modifier = Modifier.fillMaxWidth().height(4.dp).padding(top = 4.dp).clip(CircleShape)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            2 -> // Financial Ledger folder
                                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    // Registrar Direct Invoicing/Payment Controls
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text("Ledger Actions & Updates", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkMaroon)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = { showPaymentDialog = true },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Record Payment", fontSize = 11.sp)
                                                }
                                                Button(
                                                    onClick = { showInvoiceDialog = true },
                                                    colors = ButtonDefaults.buttonColors(containerColor = DarkMaroon),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Log Invoice", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }

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
                                        if (feeStatement == null || feeStatement.items.isEmpty()) {
                                            Text("No transactions logged in ledger.", fontSize = 12.sp, color = Color.Gray)
                                        } else {
                                            feeStatement.items.forEach { item ->
                                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(item.description, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                        Text(if (item.type == "PAYMENT") "Receipt: ${item.receiptNo} | ${item.date}" else item.date, fontSize = 10.sp, color = Color.Gray)
                                                    }
                                                    Text(
                                                        text = if (item.type == "PAYMENT") "- KES ${String.format("%,.0f", item.amount)}" else "+ KES ${String.format("%,.0f", item.amount)}",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (item.type == "PAYMENT") Color(0xFF2E7D32) else DarkMaroon
                                                    )
                                                }
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
                                                    Text("[${comm.documentType}] ${comm.title}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkMaroon)
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
                                        Button(onClick = { showActionForm = "POE" }, colors = ButtonDefaults.buttonColors(containerColor = ShinyGold, contentColor = Color.Black), modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                                            Text("Log Portfolio score", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Button(onClick = { showActionForm = "COMM" }, colors = ButtonDefaults.buttonColors(containerColor = DarkMaroon), modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
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
                                                        colors = ButtonDefaults.buttonColors(containerColor = ShinyGold, contentColor = Color.Black),
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
                                                        colors = ButtonDefaults.buttonColors(containerColor = DarkMaroon),
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

    // 1. Enroll Unit Dialog Form
    if (showEnrollDialog) {
        var selectedUnitCode by remember { mutableStateOf(availableUnits.firstOrNull()?.code ?: "") }
        var unitExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showEnrollDialog = false },
            title = { Text("Enroll Student in Unit Competency", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select Unit from Roster:")
                    if (availableUnits.isEmpty()) {
                        Text("No units configured in Curriculum. Please add units first.", color = Color.Red, fontSize = 12.sp)
                    } else {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { unitExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                val u = availableUnits.find { it.code == selectedUnitCode }
                                Text(if (u != null) "${u.code} - ${u.name}" else "Select Unit", color = DarkMaroon)
                            }
                            DropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                                availableUnits.forEach { unit ->
                                    DropdownMenuItem(
                                        text = { Text("${unit.code} - ${unit.name}") },
                                        onClick = {
                                            selectedUnitCode = unit.code
                                            unitExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedUnitCode.isNotEmpty()) {
                            onEnrollUnit(selectedUnitCode)
                            showEnrollDialog = false
                            Toast.makeText(context, "Enrolled in unit successfully!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkMaroon),
                    enabled = selectedUnitCode.isNotEmpty()
                ) {
                    Text("Enroll")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEnrollDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 2. Log Attendance Dialog Form
    if (showAttendanceDialog) {
        var selectedUnitCode by remember { mutableStateOf(grades.firstOrNull()?.unitCode ?: "") }
        var unitExpanded by remember { mutableStateOf(false) }
        var dateStr by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
        var status by remember { mutableStateOf("PRESENT") }

        AlertDialog(
            onDismissRequest = { showAttendanceDialog = false },
            title = { Text("Record Attendance Log", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select Unit:")
                    if (grades.isEmpty()) {
                        Text("Student is not enrolled in any units.", color = Color.Red, fontSize = 12.sp)
                    } else {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { unitExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                val u = grades.find { it.unitCode == selectedUnitCode }
                                Text(if (u != null) "${u.unitCode} - ${u.unitName}" else "Select Enrolled Unit", color = DarkMaroon)
                            }
                            DropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                                grades.forEach { grade ->
                                    DropdownMenuItem(
                                        text = { Text("${grade.unitCode} - ${grade.unitName}") },
                                        onClick = {
                                            selectedUnitCode = grade.unitCode
                                            unitExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = dateStr,
                        onValueChange = { dateStr = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Status: ")
                        RadioButton(selected = status == "PRESENT", onClick = { status = "PRESENT" })
                        Text("Present ")
                        RadioButton(selected = status == "ABSENT", onClick = { status = "ABSENT" })
                        Text("Absent")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedUnitCode.isNotEmpty()) {
                            onRecordAttendance(selectedUnitCode, dateStr.trim(), status)
                            showAttendanceDialog = false
                            Toast.makeText(context, "Attendance logged successfully!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkMaroon),
                    enabled = selectedUnitCode.isNotEmpty()
                ) {
                    Text("Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAttendanceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 3. Edit Unit Marks Dialog Form
    showEditMarksUnitCode?.let { unitCode ->
        val matchingGrade = grades.find { it.unitCode == unitCode }
        var catStr by remember { mutableStateOf(matchingGrade?.catMark?.toString() ?: "") }
        var examStr by remember { mutableStateOf(matchingGrade?.examMark?.toString() ?: "") }

        AlertDialog(
            onDismissRequest = { showEditMarksUnitCode = null },
            title = { Text("Update Marks for $unitCode", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(matchingGrade?.unitName ?: "", fontSize = 12.sp, color = DoveGray)
                    OutlinedTextField(
                        value = catStr,
                        onValueChange = { catStr = it },
                        label = { Text("CAT Score (Max 30)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = examStr,
                        onValueChange = { examStr = it },
                        label = { Text("Exam Score (Max 70)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cat = catStr.toDoubleOrNull()
                        val exam = examStr.toDoubleOrNull()
                        onUpdateGrades(unitCode, cat, exam)
                        showEditMarksUnitCode = null
                        Toast.makeText(context, "Academic grades updated!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkMaroon)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditMarksUnitCode = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 4. Record Payment Dialog Form
    if (showPaymentDialog) {
        var amountStr by remember { mutableStateOf("") }
        var receiptNo by remember { mutableStateOf("") }
        var dateStr by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
        var desc by remember { mutableStateOf("Tuition Fee Installment Payment") }

        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = { Text("Record Fee Payment Receipt", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = amountStr, onValueChange = { amountStr = it }, label = { Text("Amount (KES)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = receiptNo, onValueChange = { receiptNo = it }, label = { Text("Receipt Number (Must be Unique)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = dateStr, onValueChange = { dateStr = it }, label = { Text("Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountStr.toDoubleOrNull()
                        if (amt != null && receiptNo.isNotEmpty()) {
                            onRecordPayment(amt, receiptNo.trim(), dateStr.trim(), desc.trim()) { success ->
                                if (success) {
                                    showPaymentDialog = false
                                    Toast.makeText(context, "Payment posted, student balance adjusted!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Error: Receipt number already exists!", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    enabled = amountStr.isNotEmpty() && receiptNo.isNotEmpty()
                ) {
                    Text("Record")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentDialog = false }) { Text("Cancel") }
            }
        )
    }

    // 5. Log Tuition Invoice Dialog Form
    if (showInvoiceDialog) {
        var amountStr by remember { mutableStateOf("26400") }
        var dateStr by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
        var desc by remember { mutableStateOf("Tuition Invoice - Year 1 Sem 2") }

        AlertDialog(
            onDismissRequest = { showInvoiceDialog = false },
            title = { Text("Log New Tuition Semester Invoice", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = amountStr, onValueChange = { amountStr = it }, label = { Text("Invoice Amount (KES)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = dateStr, onValueChange = { dateStr = it }, label = { Text("Billing Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountStr.toDoubleOrNull()
                        if (amt != null) {
                            onRecordInvoice(amt, dateStr.trim(), desc.trim())
                            showInvoiceDialog = false
                            Toast.makeText(context, "Tuition billed, student balance adjusted!", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkMaroon),
                    enabled = amountStr.isNotEmpty()
                ) {
                    Text("Log Invoice")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInvoiceDialog = false }) { Text("Cancel") }
            }
        )
    }
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
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkMaroon)
            HorizontalDivider(color = LightDoveGray)
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
        Text(label, fontSize = 12.sp, color = DoveGray, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddStudentDialog(
    coursesList: List<Course>,
    onDismiss: () -> Unit,
    onAddStudent: (Student) -> Unit
) {
    var admissionNo by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("M") }
    var dob by remember { mutableStateOf("2005-01-01") }
    var phone by remember { mutableStateOf("") }
    var courseCode by remember { mutableStateOf(coursesList.firstOrNull()?.code ?: "DICT") }
    var courseExpanded by remember { mutableStateOf(false) }
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
                
                Text("Course Code:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { courseExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(courseCode, color = DarkMaroon)
                    }
                    DropdownMenu(expanded = courseExpanded, onDismissRequest = { courseExpanded = false }) {
                        coursesList.forEach { crs ->
                            DropdownMenuItem(
                                text = { Text(crs.code + " - " + crs.name) },
                                onClick = {
                                    courseCode = crs.code
                                    courseExpanded = false
                                }
                            )
                        }
                    }
                }

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
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkMaroon)
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
