package com.erns.alertauni.screen.course

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erns.alertauni.data.repository.TeacherCourseRepository
import com.erns.alertauni.domain.manager.DataStoreHelper
import com.erns.alertauni.util.CourseCodeUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TeacherCourseUiState(
    val courseId: String = "CAT-101",
    val courseCode: String = "000001",
    val courseName: String = "Cargando curso...",
    val semester: String = "2026-B",
    val enrollmentCode: String = "",
    val qrBitmap: Bitmap? = null,
    val isLoading: Boolean = false,
    val isEnrollmentOpen: Boolean = true,
    val teacherName: String = "Docente",
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class TeacherCourseViewModel @Inject constructor(
    private val teacherCourseRepository: TeacherCourseRepository,
    private val dataStoreHelper: DataStoreHelper
) : ViewModel() {
    private val TAG = "TeacherCourseViewModel"

    private val _uiState = MutableStateFlow(TeacherCourseUiState())
    val uiState: StateFlow<TeacherCourseUiState> = _uiState.asStateFlow()

    init {
        loadTeacherInfo()
        loadSavedCourseData("CAT-101")
    }

    private fun loadTeacherInfo() {
        viewModelScope.launch {
            val firstname = dataStoreHelper.getFirstname()
            val surname = dataStoreHelper.getSurname()
            val fullName = "$firstname $surname".trim()
            if (fullName.isNotBlank()) {
                _uiState.update { it.copy(teacherName = fullName) }
            }
        }
    }

    private fun loadSavedCourseData(courseId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            teacherCourseRepository.getCourseDetails(courseId)
                .onSuccess { details ->
                    val codeToUse = details.classCode.ifBlank { "A8K92X" }
                    val bitmap = CourseCodeUtil.generateQrCodeBitmap(codeToUse, 512)
                    Log.d(TAG, "Course details loaded from DB: name=${details.courseName}, code=${details.courseCode}, classCode=$codeToUse")

                    _uiState.update { current ->
                        current.copy(
                            courseId = details.courseCatalogId.ifBlank { courseId },
                            courseCode = details.courseCode.ifBlank { "000001" },
                            courseName = details.courseName.ifBlank { "Curso 1 - Programación Avanzada" },
                            semester = details.semester.ifBlank { "2026-B" },
                            enrollmentCode = codeToUse,
                            qrBitmap = bitmap,
                            isLoading = false
                        )
                    }
                }
                .onFailure { ex ->
                    Log.e(TAG, "Error loading course details from DB: ${ex.message}", ex)
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Error cargando BD: ${ex.message}") }
                }
        }
    }

    fun generateNewEnrollmentCode(courseId: String = "") {
        val targetCourseId = courseId.ifBlank { _uiState.value.courseId }
        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

        viewModelScope.launch {
            val newCode = CourseCodeUtil.generateRandomCode(6)
            val bitmap = CourseCodeUtil.generateQrCodeBitmap(newCode, 512)
            val result = teacherCourseRepository.updateCourseEnrollmentCode(targetCourseId, newCode)

            result.onSuccess {
                _uiState.update { current ->
                    current.copy(
                        courseId = targetCourseId,
                        enrollmentCode = newCode,
                        qrBitmap = bitmap,
                        isLoading = false,
                        isEnrollmentOpen = true,
                        successMessage = "Código $newCode cambiado exitosamente"
                    )
                }
            }.onFailure { ex ->
                _uiState.update { current ->
                    current.copy(
                        courseId = targetCourseId,
                        errorMessage = "Error al actualizar código: ${ex.message}",
                        isLoading = false
                    )
                }
            }
        }
    }
}
