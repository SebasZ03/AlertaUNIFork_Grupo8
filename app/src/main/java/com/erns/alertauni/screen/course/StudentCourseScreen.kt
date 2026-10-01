package com.erns.alertauni.screen.course

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erns.alertauni.data.model.StudentEnrollment

// Paleta de colores del prototipo de Figma
private val FigmaGold = Color(0xFFC59A27)
private val FigmaRed = Color(0xFFEF5350)
private val FigmaGreen = Color(0xFF2E7D32)
private val FigmaBackground = Color(0xFFF9F9F8)

/**
 * Pantalla completa de incorporación de cursos para el Estudiante (Estado 1 de Figma).
 */
@Composable
fun StudentCourseScreen(
    studentName: String = "Juan",
    studentEnrollment: StudentEnrollment? = null,
    isEnrolledSuccess: Boolean = false,
    errorMessage: String? = null,
    onFindCourse: (String) -> Unit = {},
    onEnrollCourse: (String) -> Unit = {},
    onOpenQr: () -> Unit = {}
) {
    var inputText by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        containerColor = FigmaBackground,
        topBar = {
            // Header Superior: "Hola, Juan 👋" + Campana de notificaciones
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hola, $studentName 👋",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    )
                    Text(
                        text = "¿Que quieres Hacer hoy?",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    )
                }

                // Botón Notificación
                IconButton(
                    onClick = { },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White, CircleShape)
                ) {
                    BadgedBox(
                        badge = {
                            Badge(
                                containerColor = FigmaGold,
                                modifier = Modifier.size(8.dp)
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notificaciones",
                            tint = Color.Black
                        )
                    }
                }
            }
        },
        bottomBar = {
            // Navigation Bar Inferior de Figma (Inicio, Buscar, Mis cursos, Perfil)
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                    label = { Text("Inicio", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = FigmaGold,
                        selectedTextColor = FigmaGold,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                    label = { Text("Buscar", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = FigmaGold,
                        selectedTextColor = FigmaGold,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Mis cursos") },
                    label = { Text("Mis cursos", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = FigmaGold,
                        selectedTextColor = FigmaGold,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
                    label = { Text("Perfil", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = FigmaGold,
                        selectedTextColor = FigmaGold,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            when {
                // ESTADO 5: Matrícula Exitosa
                isEnrolledSuccess -> {
                    Text(
                        text = "Confirmar Curso",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    studentEnrollment?.let { course ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Nombre: ${course.courseName}", fontWeight = FontWeight.Medium)
                                Text("Cód: ${course.courseCode}", fontWeight = FontWeight.Medium)
                                Text("Semestre: ${course.semester}", fontWeight = FontWeight.Medium)
                                Text("Docente: ${course.firstname} ${course.surname}", fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Éxito",
                            tint = FigmaGreen,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Te has incorporado exitosamente",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FigmaGold),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Ir al muro de publicaciones", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // ESTADO 4: Confirmar Curso Encontrado
                studentEnrollment != null -> {
                    Text(
                        text = "Confirmar Curso",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Nombre: ${studentEnrollment.courseName}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Cód: ${studentEnrollment.courseCode}", fontSize = 14.sp)
                            Text("Semestre: ${studentEnrollment.semester}", fontSize = 14.sp)
                            Text("Docente: ${studentEnrollment.firstname} ${studentEnrollment.surname}", fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { onEnrollCourse(studentEnrollment.courseId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FigmaGold),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Confirmar Matrícula", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FigmaRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancelar", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                // ESTADO 1: Ingresar a un Curso (Pantalla Principal)
                else -> {
                    Text(
                        text = "Ingresar a un Curso",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ingresar código de clase", color = Color.Gray, fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FigmaGold,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    if (!errorMessage.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "⚠️ $errorMessage",
                            color = FigmaRed,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (inputText.isNotBlank()) onFindCourse(inputText)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FigmaGold),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Buscar Curso", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        // Botón Escáner QR
                        OutlinedIconButton(
                            onClick = onOpenQr,
                            modifier = Modifier
                                .size(50.dp)
                                .background(Color.White, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
                        ) {
                            Text("QR", fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// PREVIEWS DE FIGMA
// =========================================================================

@Preview(name = "Figma Estado 1: Pantalla Principal", showBackground = true, showSystemUi = true)
@Composable
fun StudentCourseScreenEstado1Preview() {
    MaterialTheme {
        StudentCourseScreen(
            studentName = "Juan"
        )
    }
}