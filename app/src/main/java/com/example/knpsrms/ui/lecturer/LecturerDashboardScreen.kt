package com.example.knpsrms.ui.lecturer

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
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
import com.example.knpsrms.data.models.Lecturer
import com.example.knpsrms.data.models.StudentRosterItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LecturerDashboardScreen(
    employeeNo: String,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { DataRepository(context) }
    val viewModel: LecturerViewModel = viewModel { LecturerViewModel(repository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(employeeNo) {
        viewModel.loadLecturerData(employeeNo)
    }

    val isUnitSelected = when (val uiState = state) {
        is LecturerUiState.Success -> uiState.selectedUnitCode != null
        else -> false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isUnitSelected) "Unit Details" else "Lecturer Portal", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF5C1324)),
                navigationIcon = {
                    if (isUnitSelected) {
                        IconButton(onClick = { viewModel.clearSelectedUnit() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = "Logout", tint = Color.White)
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
                is LecturerUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is LecturerUiState.Error -> {
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
                is LecturerUiState.Success -> {
                    if (uiState.selectedUnitCode == null) {
                        LecturerHome(
                            lecturer = uiState.lecturer,
                            units = uiState.units,
                            onUnitSelect = { viewModel.selectUnit(it) }
                        )
                    } else {
                        val unit = uiState.units.first { it.code == uiState.selectedUnitCode }
                        UnitRosterView(
                            unitCode = uiState.selectedUnitCode,
                            unitName = unit.name,
                            roster = uiState.roster,
                            onSubmitGrade = { enrollmentId, cat, exam ->
                                viewModel.submitGrades(enrollmentId, cat, exam, uiState.selectedUnitCode)
                            },
                            onRecordAttendance = { enrollmentId, status ->
                                viewModel.recordAttendance(enrollmentId, status, uiState.selectedUnitCode)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LecturerHome(
    lecturer: Lecturer,
    units: List<DataRepository.UnitEntity>,
    onUnitSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Welcome Card
        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF5E6E8)), // Light maroon tint
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = lecturer.firstName.take(1) + lecturer.lastName.take(1),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5C1324) // KNP Dark Maroon
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "WELCOME, MR. ${lecturer.lastName.uppercase()}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Employee ID: ${lecturer.employeeNo}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Dept: ${lecturer.department} Department",
                        fontSize = 12.sp,
                        color = Color(0xFFD4AF37), // KNP Shiny Gold
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Text(
            text = "Your Assigned Course Units",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (units.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No units allocated to your profile.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(units) { unit ->
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUnitSelect(unit.code) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFE8EAF6), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = unit.code,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1A237E)
                                    )
                                }
                                Text(
                                    text = "Class: ${unit.courseCode}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = unit.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap to manage grades and class roll",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UnitRosterView(
    unitCode: String,
    unitName: String,
    roster: List<StudentRosterItem>,
    onSubmitGrade: (Int, Double?, Double?) -> Unit,
    onRecordAttendance: (Int, String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Grade Entry", "Attendance Log")
    val context = LocalContext.current

    var activeRosterItemForGradeDialog by remember { mutableStateOf<StudentRosterItem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Course title header card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(unitName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("Code: $unitCode | Enrolled: ${roster.size} Students", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Tab Selector
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary, // KNP Dark Maroon / Shiny Gold
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .padding(bottom = 12.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                val selected = selectedTab == index
                Tab(
                    selected = selected,
                    onClick = { selectedTab = index },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        if (roster.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No students currently enrolled in this unit.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    0 -> // Grade sheet view
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(roster) { student ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { activeRosterItemForGradeDialog = student }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(student.studentName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                            Text("ADM: ${student.studentId}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("CAT: ${student.catMark?.let { String.format("%.1f", it) } ?: "-"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("Exam: ${student.examMark?.let { String.format("%.1f", it) } ?: "-"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Icon(
                                                Icons.Filled.Create,
                                                contentDescription = "Edit grades",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    1 -> // Attendance rol-call checker
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(roster) { student ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(student.studentName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                            Text("Attendance: ${student.presentCount} / ${student.totalClasses} classes", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Register Present button
                                            Button(
                                                onClick = {
                                                    onRecordAttendance(student.enrollmentId, "PRESENT")
                                                    Toast.makeText(context, "Marked ${student.studentName} Present", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8F5E9), contentColor = Color(0xFF2E7D32)),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Text("Present", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                            // Register Absent button
                                            Button(
                                                onClick = {
                                                    onRecordAttendance(student.enrollmentId, "ABSENT")
                                                    Toast.makeText(context, "Marked ${student.studentName} Absent", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE), contentColor = Color(0xFFC62828)),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Text("Absent", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

    // Modal dialogue to input/edit grades
    activeRosterItemForGradeDialog?.let { student ->
        var catInput by remember { mutableStateOf(student.catMark?.toString() ?: "") }
        var examInput by remember { mutableStateOf(student.examMark?.toString() ?: "") }
        var inputError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { activeRosterItemForGradeDialog = null },
            title = { Text("Input Grades", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Student: ${student.studentName}\nID: ${student.studentId}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    OutlinedTextField(
                        value = catInput,
                        onValueChange = { catInput = it },
                        label = { Text("CAT Mark (Max 30)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = examInput,
                        onValueChange = { examInput = it },
                        label = { Text("Exam Mark (Max 70)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    inputError?.let {
                        Text(it, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cat = catInput.toDoubleOrNull()
                        val exam = examInput.toDoubleOrNull()

                        if (catInput.isNotEmpty() && (cat == null || cat < 0.0 || cat > 30.0)) {
                            inputError = "CAT mark must be a valid number between 0 and 30"
                            return@Button
                        }
                        if (examInput.isNotEmpty() && (exam == null || exam < 0.0 || exam > 70.0)) {
                            inputError = "Exam mark must be a valid number between 0 and 70"
                            return@Button
                        }

                        onSubmitGrade(student.enrollmentId, cat, exam)
                        Toast.makeText(context, "Grades updated for ${student.studentName}", Toast.LENGTH_SHORT).show()
                        activeRosterItemForGradeDialog = null
                    }
                ) {
                    Text("Save Grades")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeRosterItemForGradeDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
