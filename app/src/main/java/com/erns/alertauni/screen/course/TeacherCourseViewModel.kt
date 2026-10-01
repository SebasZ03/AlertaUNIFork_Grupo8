package com.erns.alertauni.screen.course

import android.graphics.Bitmap
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
    val courseName: String = "Curso 1 - Programación Avanzada",
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

    private val _uiState = MutableStateFlow(TeacherCourseUiState())
    val uiState: StateFlow<TeacherCourseUiState> = _uiState.asStateFlow()

    init {
        loadTeacherInfo()
        generateNewEnrollmentCode("CAT-101")
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
                        successMessage = "Código $newCode generado exitosamente"
                    )
                }
            }.onFailure {
                _uiState.update { current ->
                    current.copy(
                        courseId = targetCourseId,
                        enrollmentCode = newCode,
                        qrBitmap = bitmap,
                        isLoading = false,
                        isEnrollmentOpen = true,
                        errorMessage = "Código $newCode generado localmente"
                    )
                }
            }
        }
    }
}
