package com.erns.alertauni.screen.course

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.erns.alertauni.data.model.StudentEnrollment
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

// Paleta de colores oficial de Figma
private val FigmaGold = Color(0xFFC59A27)
private val FigmaRed = Color(0xFFEF5350)
private val FigmaGreen = Color(0xFF2E7D32)
private val FigmaBackground = Color(0xFFF9F9F8)
private val FigmaCardBg = Color(0xFFF4F4F4)

/**
 * Estados visuales del flujo de incorporación (Mapeo Figma).
 */
enum class CourseUiState {
    INPUT_CODE,         // Estado 1 / 6a / 6c
    SCAN_QR,            // Estado 2
    LOADING_QR,         // Estado 3
    FOUND_QR,           // Estado 4-Prev
    CONFIRM_COURSE,     // Estado 4
    SUCCESS,            // Estado 5
    ALREADY_ENROLLED    // Estado 6b
}

@Composable
fun StudentCourseRoute(
    viewModel: CourseViewModel = hiltViewModel(),
    onGoToCourse: (String) -> Unit = {},
    snackbarHostState: SnackbarHostState? = null,
    onFabActionReady: (() -> Unit) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val studentEnrollment by viewModel.studentEnrollment.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val studentName by viewModel.username.collectAsState()

    StudentCourseScreen(
        studentName = studentName,
        uiState = uiState,
        studentEnrollment = studentEnrollment,
        errorMessage = errorMessage,
        onFindCourse = { code -> viewModel.findCourseByCode(code) },
        onProcessQr = { qrContent -> viewModel.processQrScanResult(qrContent) },
        onEnrollCourse = { courseId -> viewModel.enrollStudent(courseId) },
        onOpenQr = { viewModel.openQrScanner() },
        onSwitchToManual = { viewModel.resetToManualInput() },
        onGoToCourse = {
            val courseId = studentEnrollment?.courseId?.ifBlank { studentEnrollment?.course_catalog_id } ?: ""
            onGoToCourse(courseId)
        },
        snackbarHostState = snackbarHostState,
        onFabActionReady = onFabActionReady
    )
}

@Composable
fun StudentCourseScreen(
    studentName: String = "Juan",
    uiState: CourseUiState = CourseUiState.INPUT_CODE,
    studentEnrollment: StudentEnrollment? = null,
    errorMessage: String? = null,
    onFindCourse: (String) -> Unit = {},
    onProcessQr: (String) -> Unit = {},
    onEnrollCourse: (String) -> Unit = {},
    onOpenQr: () -> Unit = {},
    onSwitchToManual: () -> Unit = {},
    onGoToCourse: () -> Unit = {},
    snackbarHostState: SnackbarHostState? = null,
    onFabActionReady: (() -> Unit) -> Unit = {}
) {
    var inputText by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(2) }

    Scaffold(
        containerColor = FigmaBackground,
        topBar = {
            // Header Superior: Saludo y Notificaciones
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
            // Navigation Bar Inferior
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                val items = listOf(
                    "Inicio" to Icons.Default.Home,
                    "Buscar" to Icons.Default.Search,
                    "Mis cursos" to Icons.Default.DateRange,
                    "Perfil" to Icons.Default.Person
                )
                items.forEachIndexed { index, pair ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(pair.second, contentDescription = pair.first) },
                        label = { Text(pair.first, fontSize = 11.sp) },
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            when (uiState) {
                // ESTADO 1 / 6a / 6c: Ingresar Código / Errores
                CourseUiState.INPUT_CODE -> {
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
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
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
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FigmaGold
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (errorMessage == "Error de conexión.") "Reintentar" else "Buscar Curso",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedIconButton(
                            onClick = onOpenQr,
                            modifier = Modifier
                                .size(50.dp)
                                .background(Color.White, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "QR",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                        }
                    }
                }

                // ESTADO 2, 3 y 4-Prev: Escáner QR y Carga
                CourseUiState.SCAN_QR, CourseUiState.LOADING_QR, CourseUiState.FOUND_QR -> {
                    Text(
                        text = "Escanear QR",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState == CourseUiState.SCAN_QR) {
                            QrCameraScanner(
                                modifier = Modifier.fillMaxSize(),
                                onQrCodeScanned = onProcessQr
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFE0E0E0), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(180.dp)
                                        .background(Color.White, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "QR",
                                        fontSize = 48.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.Black
                                    )
                                }
                            }
                        }

                        if (uiState == CourseUiState.LOADING_QR) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = Color.White)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Localizando\ncurso ...", color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                }
                            }
                        }

                        if (uiState == CourseUiState.FOUND_QR) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = FigmaGreen, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Curso Encontrado\nRedirigiendo", color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FigmaGold),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Buscar Curso", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onSwitchToManual,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD3D3D3)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Ingresar Código Manual", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                // ESTADO 4: Confirmar Curso
                CourseUiState.CONFIRM_COURSE -> {
                    Text(
                        text = "Confirmar Curso",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    studentEnrollment?.let { course ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Nombre: ${course.courseName}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Cód: ${course.courseCode}", fontSize = 14.sp)
                                Text("Semestre: ${course.semester}", fontSize = 14.sp)
                                Text("Docente: ${course.firstname} ${course.surname}", fontSize = 14.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { studentEnrollment?.let { onEnrollCourse(it.course_catalog_id.ifBlank { it.courseId }) } },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FigmaGold),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Confirmar Matrícula", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onSwitchToManual,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FigmaRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancelar", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                // ESTADO 5: Matrícula Exitosa
                CourseUiState.SUCCESS -> {
                    Text(
                        text = "Confirmar Curso",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    studentEnrollment?.let { course ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = FigmaCardBg),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Nombre: ${course.courseName}", fontWeight = FontWeight.Bold)
                                Text("Cód: ${course.courseCode}")
                                Text("Semestre: ${course.semester}")
                                Text("Docente: ${course.firstname} ${course.surname}")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Éxito", tint = FigmaGreen, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Te has incorporado exitosamente", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = onGoToCourse,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FigmaGold),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Ir al muro de publicaciones", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // ESTADO 6b: Ya perteneces a este curso
                CourseUiState.ALREADY_ENROLLED -> {
                    Text(
                        text = "Ingresar a un Curso",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Curso: ${studentEnrollment?.courseName ?: "Curso 1"}", fontWeight = FontWeight.Bold)
                            Text("Ya perteneces a este curso", color = Color.Gray, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onGoToCourse,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FigmaGold),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Ir al Curso", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun QrCameraScanner(
    modifier: Modifier = Modifier,
    onQrCodeScanned: (String) -> Unit
) {
    val isPreview = LocalInspectionMode.current
    if (isPreview) {
        Box(
            modifier = modifier.background(Color(0xFFE0E0E0), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .background(Color.White, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "QR",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black
                )
            }
        }
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = CameraPreview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val barcodeScanner = BarcodeScanning.getClient()
                    var isDetected = false

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                        @OptIn(ExperimentalGetImage::class)
                        val mediaImage = imageProxy.image
                        if (mediaImage != null && !isDetected) {
                            val image = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )
                            barcodeScanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    for (barcode in barcodes) {
                                        val rawValue = barcode.rawValue
                                        if (!rawValue.isNullOrEmpty() && !isDetected) {
                                            isDetected = true
                                            cameraProvider.unbindAll()
                                            onQrCodeScanned(rawValue)
                                            break
                                        }
                                    }
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        Log.e("QrCameraScanner", "Error al inicializar la cámara", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(Color.DarkGray, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Se requiere permiso de cámara para escanear el código QR.",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = FigmaGold)
                ) {
                    Text("Conceder Permiso", color = Color.White)
                }
            }
        }
    }
}

// =========================================================================
// CATÁLOGO COMPLETO DE PREVIEWS PARA CAPTURAS DEL INFORME
// =========================================================================

private val mockCourse = StudentEnrollment(
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
)

@Preview(name = "1. Figma Estado 1: Inicio", showBackground = true, showSystemUi = true)
@Composable
fun PreviewEstado1() {
    MaterialTheme { StudentCourseScreen(uiState = CourseUiState.INPUT_CODE) }
}

@Preview(name = "2. Figma Estado 2: Escanear QR", showBackground = true, showSystemUi = true)
@Composable
fun PreviewEstado2() {
    MaterialTheme { StudentCourseScreen(uiState = CourseUiState.SCAN_QR) }
}

@Preview(name = "3. Figma Estado 3: Cargando QR", showBackground = true, showSystemUi = true)
@Composable
fun PreviewEstado3() {
    MaterialTheme { StudentCourseScreen(uiState = CourseUiState.LOADING_QR) }
}

@Preview(name = "4. Figma Estado 4-Prev: Redirigiendo QR", showBackground = true, showSystemUi = true)
@Composable
fun PreviewEstado4Prev() {
    MaterialTheme { StudentCourseScreen(uiState = CourseUiState.FOUND_QR) }
}

@Preview(name = "5. Figma Estado 4: Confirmar Matrícula", showBackground = true, showSystemUi = true)
@Composable
fun PreviewEstado4() {
    MaterialTheme {
        StudentCourseScreen(
            uiState = CourseUiState.CONFIRM_COURSE,
            studentEnrollment = mockCourse
        )
    }
}

@Preview(name = "6. Figma Estado 5: Matrícula Exitosa", showBackground = true, showSystemUi = true)
@Composable
fun PreviewEstado5() {
    MaterialTheme {
        StudentCourseScreen(
            uiState = CourseUiState.SUCCESS,
            studentEnrollment = mockCourse
        )
    }
}

@Preview(name = "7. Figma Estado 6a: Error Código Inexistente", showBackground = true, showSystemUi = true)
@Composable
fun PreviewEstado6a() {
    MaterialTheme {
        StudentCourseScreen(
            uiState = CourseUiState.INPUT_CODE,
            errorMessage = "El código no existe o las inscripciones estan cerradas."
        )
    }
}

@Preview(name = "8. Figma Estado 6b: Ya Registrado", showBackground = true, showSystemUi = true)
@Composable
fun PreviewEstado6b() {
    MaterialTheme {
        StudentCourseScreen(
            uiState = CourseUiState.ALREADY_ENROLLED,
            studentEnrollment = mockCourse
        )
    }
}

@Preview(name = "9. Figma Estado 6c: Error de Conexión", showBackground = true, showSystemUi = true)
@Composable
fun PreviewEstado6c() {
    MaterialTheme {
        StudentCourseScreen(
            uiState = CourseUiState.INPUT_CODE,
            errorMessage = "Error de conexión."
        )
    }
}