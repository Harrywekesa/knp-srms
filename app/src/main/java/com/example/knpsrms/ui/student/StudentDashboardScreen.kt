package com.example.knpsrms.ui.student

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.knpsrms.data.DataRepository
import com.example.knpsrms.data.models.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboardScreen(
    admissionNo: String,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { DataRepository(context) }
    val viewModel: StudentViewModel = viewModel { StudentViewModel(repository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(admissionNo) {
        viewModel.loadStudentData(admissionNo)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("KNP StudentHub", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF5C1324)),
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = Color.White
                        )
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
                is StudentUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is StudentUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = uiState.message,
                            fontSize = 16.sp,
                            color = Color.Red,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                is StudentUiState.Success -> {
                    StudentDashboard(
                        student = uiState.student,
                        grades = uiState.grades,
                        feeStatement = uiState.feeStatement,
                        attendance = uiState.attendance
                    )
                }
            }
        }
    }
}

@Composable
private fun StudentDashboard(
    student: Student,
    grades: List<Grade>,
    feeStatement: FeeStatement,
    attendance: List<AttendanceSummary>
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Grades", "Fee Statement", "Attendance")

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
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circle Profile placeholder
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF5E6E8)), // Light maroon tint
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = student.firstName.take(1) + student.lastName.take(1),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5C1324) // Dark Maroon
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = student.fullName.uppercase(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "ADM: ${student.admissionNo}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Course: ${student.courseCode} Program",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary, // Dark Maroon in light, Gold in dark
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Summary Quick Metrics
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Compute overall attendance
            val totalClasses = attendance.sumOf { it.totalClasses }
            val presentCount = attendance.sumOf { it.presentCount }
            val attendancePercent = if (totalClasses > 0) {
                (presentCount.toDouble() / totalClasses.toDouble()) * 100.0
            } else 0.0
            val isEligible = attendancePercent >= 75.0

            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Attendance", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format("%.1f%%", attendancePercent),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isEligible) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isEligible) "Cleared" else "Blocked (Exam)",
                        fontSize = 10.sp,
                        color = if (isEligible) Color(0xFF2E7D32) else Color(0xFFC62828),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Fees Balance", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format("KES %,.0f", student.feeBalance),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (student.feeBalance == 0.0) Color(0xFF2E7D32) else Color(0xFFE65100)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (student.feeBalance == 0.0) "Fully Paid" else "Incomplete Payment",
                        fontSize = 10.sp,
                        color = if (student.feeBalance == 0.0) Color(0xFF2E7D32) else Color(0xFFE65100),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Tab Row selector
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary, // Dark Maroon / Shiny Gold
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

        // Tab Content view
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedTab) {
                0 -> GradesView(grades)
                1 -> FeeStatementView(feeStatement)
                2 -> AttendanceView(attendance)
            }
        }
    }
}

@Composable
private fun GradesView(grades: List<Grade>) {
    if (grades.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No academic grades recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(grades) { grade ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = grade.unitName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Grade Badge
                        val badgeColor = when (grade.gradeLetter) {
                            "A" -> Color(0xFFE8F5E9)
                            "B", "C" -> Color(0xFFE3F2FD)
                            "PASS" -> Color(0xFFFFF3E0)
                            else -> Color(0xFFFFEBEE)
                        }
                        val textColor = when (grade.gradeLetter) {
                            "A" -> Color(0xFF2E7D32)
                            "B", "C" -> Color(0xFF1565C0)
                            "PASS" -> Color(0xFFE65100)
                            else -> Color(0xFFC62828)
                        }
                        Box(
                            modifier = Modifier
                                .background(badgeColor, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Grade ${grade.gradeLetter}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Unit Code: ${grade.unitCode}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("CAT Mark (30)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = grade.catMark?.let { String.format("%.1f", it) } ?: "Pending",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (grade.catMark != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Exam Mark (70)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = grade.examMark?.let { String.format("%.1f", it) } ?: "Pending",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (grade.examMark != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Aggregate (100)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = grade.aggregateMark?.let { String.format("%.1f", it) } ?: "Pending",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (grade.aggregateMark != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeeStatementView(feeStatement: FeeStatement) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Stats Summary header
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Invoiced", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(String.format("KES %,.0f", feeStatement.totalInvoiced), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Column {
                    Text("Total Paid", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(String.format("KES %,.0f", feeStatement.totalPaid), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Balance Due", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(String.format("KES %,.0f", feeStatement.balance), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                }
            }
        }

        // Ledger List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(feeStatement.items) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Color status dot
                        val isPayment = item.type == "PAYMENT"
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(if (isPayment) Color(0xFF2E7D32) else Color(0xFF78909C), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.description,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isPayment) "Receipt: ${item.receiptNo} | Date: ${item.date}" else "Date: ${item.date}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = if (isPayment) "- KES ${String.format("%,.0f", item.amount)}" else "+ KES ${String.format("%,.0f", item.amount)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPayment) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceView(attendance: List<AttendanceSummary>) {
    if (attendance.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No class attendance logs recorded.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(attendance) { summary ->
            val isCleared = summary.percentage >= 75.0
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = summary.unitName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Code: ${summary.unitCode}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = String.format("%.1f%%", summary.percentage),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCleared) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Progress Bar
                    LinearProgressIndicator(
                        progress = { (summary.percentage / 100.0).toFloat() },
                        color = if (isCleared) Color(0xFF4CAF50) else Color(0xFFF44336),
                        trackColor = Color(0xFFE0E0E0),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Attended: ${summary.presentCount} / ${summary.totalClasses} Lectures",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isCleared) "Eligible for Exams" else "Attendance Too Low",
                            fontSize = 10.sp,
                            color = if (isCleared) Color(0xFF2E7D32) else Color(0xFFC62828),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
