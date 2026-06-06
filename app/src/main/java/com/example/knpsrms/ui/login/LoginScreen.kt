package com.example.knpsrms.ui.login

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.knpsrms.data.DataRepository
import com.example.knpsrms.data.models.User

@Composable
fun LoginScreen(
    onLoginSuccess: (User) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { DataRepository(context) }
    val loginViewModel: LoginViewModel = viewModel { LoginViewModel(repository) }
    val state by loginViewModel.uiState.collectAsStateWithLifecycle()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("STUDENT") } // "STUDENT" or "LECTURER"

    // Handle authentication states
    LaunchedEffect(state) {
        when (val loginState = state) {
            is LoginUiState.Success -> {
                onLoginSuccess(loginState.user)
                loginViewModel.resetState()
            }
            is LoginUiState.Error -> {
                Toast.makeText(context, loginState.message, Toast.LENGTH_LONG).show()
                loginViewModel.resetState()
            }
            else -> {}
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF5C1324), // Dark Maroon
                        Color(0xFF8B2B3F),
                        Color(0xFFECEEF1)  // Light Dove Gray
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header Logo Area
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = com.example.knpsrms.R.drawable.knp_logo),
                        contentDescription = "KNP Logo",
                        modifier = Modifier
                            .size(70.dp)
                            .padding(bottom = 8.dp)
                            .clip(CircleShape)
                    )
                    Text(
                        text = "KITALE NATIONAL POLYTECHNIC",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "STUDENT RECORD MANAGEMENT SYSTEM",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFD4AF37), // Shiny Gold
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            // Main login box
            ElevatedCard(
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Sign In",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Text(
                        text = "Enter your institutional credentials",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(bottom = 20.dp)
                    )

                    // Role Select Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { role = "STUDENT" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (role == "STUDENT") MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (role == "STUDENT") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Student", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = { role = "LECTURER" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (role == "LECTURER") MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (role == "LECTURER") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Lecturer", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = { role = "ADMIN" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (role == "ADMIN") MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (role == "ADMIN") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Admin", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Username Input
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = {
                            Text(
                                when (role) {
                                    "STUDENT" -> "Admission Number"
                                    "LECTURER" -> "Employee Number"
                                    else -> "Admin ID"
                                }
                            )
                        },
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )

                    // Password Input
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                    )

                    // Submit Button
                    Button(
                        onClick = { loginViewModel.login(username, password, role) },
                        shape = RoundedCornerShape(12.dp),
                        enabled = state !is LoginUiState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (state is LoginUiState.Loading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text("Login", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Quick Demo Accounts Container
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            tint = Color(0xFF5C1324), // Dark Maroon
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Quick Demo Accounts (Tap to fill)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5C1324) // Dark Maroon
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Student Demo Click
                    TextButton(
                        onClick = {
                            username = "KNP/ICT/2024/099"
                            password = "password123"
                            role = "STUDENT"
                        },
                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Student Account", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.SemiBold)
                            Text("User: KNP/ICT/2024/099 | Pass: password123", fontSize = 11.sp, color = Color.Gray)
                        }
                    }

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

                    // Lecturer Demo Click
                    TextButton(
                        onClick = {
                            username = "KNP/LEC/402"
                            password = "password123"
                            role = "LECTURER"
                        },
                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Lecturer Account", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.SemiBold)
                            Text("User: KNP/LEC/402 | Pass: password123", fontSize = 11.sp, color = Color.Gray)
                        }
                    }

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

                    // Admin Demo Click
                    TextButton(
                        onClick = {
                            username = "KNP/ADM/001"
                            password = "password123"
                            role = "ADMIN"
                        },
                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Admin Account", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.SemiBold)
                            Text("User: KNP/ADM/001 | Pass: password123", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}
