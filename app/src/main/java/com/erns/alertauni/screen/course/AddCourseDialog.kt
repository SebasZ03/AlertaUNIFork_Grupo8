package com.erns.alertauni.screen.course

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
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

/**
 * Diálogo de incorporación a curso diseñado según el prototipo de Figma (Estados 1, 4 y 5).
 */
@Composable
fun AddCourseDialog(
    studentEnrollment: StudentEnrollment?,
    onDismiss: () -> Unit,
    onClickFindCourse: (String) -> Unit,
    onClickCourseEnroll: (String) -> Unit,
    onOpenQrScanner: () -> Unit = {},
    errorMessage: String? = null,
    isEnrolledSuccess: Boolean = false
) {
    var inputText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {}, // Botones ubicados internamente según prototipo de Figma
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                when {
                    // ESTADO 5: Matrícula Exitosa
                    isEnrolledSuccess -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Éxito",
                                tint = FigmaGreen,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Te has incorporado exitosamente",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FigmaGold),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Ir al muro de publicaciones",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // ESTADO 4: Confirmar Curso Encontrado
                    studentEnrollment != null -> {
                        Text(
                            text = "Confirmar Curso",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Nombre: ${studentEnrollment.courseName}",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Cód: ${studentEnrollment.courseCode}",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Semestre: ${studentEnrollment.semester}",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Docente: ${studentEnrollment.firstname} ${studentEnrollment.surname}",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Botón Primario: Confirmar Matrícula (Dorado)
                        Button(
                            onClick = { onClickCourseEnroll(studentEnrollment.courseId) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FigmaGold),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Confirmar Matrícula",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Botón Secundario: Cancelar (Rojo)
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FigmaRed),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Cancelar",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // ESTADO 1: Ingreso de Código y Búsqueda Manual / QR
                    else -> {
                        Text(
                            text = "Ingresar a un Curso",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Ingresar código de clase") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (!errorMessage.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "⚠️ $errorMessage",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Botón Buscar Curso (Dorado)
                            Button(
                                onClick = {
                                    if (inputText.isNotBlank()) {
                                        onClickFindCourse(inputText)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FigmaGold),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Buscar Curso",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Botón Lector QR
                            OutlinedIconButton(
                                onClick = onOpenQrScanner,
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "QR",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

// =========================================================================
// PREVIEWS OFFLINE
// =========================================================================

@Preview(name = "Figma Estado 1: Ingresar Código", showBackground = true)
@Composable
fun FigmaEstado1Preview() {
    MaterialTheme {
        AddCourseDialog(
            studentEnrollment = null,
            onDismiss = {},
            onClickFindCourse = {},
            onClickCourseEnroll = {}
        )
    }
}

@Preview(name = "Figma Estado 4: Confirmar Curso", showBackground = true)
@Composable
fun FigmaEstado4Preview() {
    MaterialTheme {
        AddCourseDialog(
            studentEnrollment = StudentEnrollment(
                courseId = "CAT-101",
                course_catalog_id = "CAT-101",
                courseCode = "000001",
                courseName = "Curso 1",
                courseType = "Obligatorio",
                groupType = "Grupo A",
                firstname = "Julio",
                surname = "Pérez",
                email = "julio@unsa.edu.pe",
                semester = "2026-B"
            ),
            onDismiss = {},
            onClickFindCourse = {},
            onClickCourseEnroll = {}
        )
    }
}

@Preview(name = "Figma Estado 5: Matrícula Exitosa", showBackground = true)
@Composable
fun FigmaEstado5Preview() {
    MaterialTheme {
        AddCourseDialog(
            studentEnrollment = null,
            onDismiss = {},
            onClickFindCourse = {},
            onClickCourseEnroll = {},
            isEnrolledSuccess = true
        )
    }
}