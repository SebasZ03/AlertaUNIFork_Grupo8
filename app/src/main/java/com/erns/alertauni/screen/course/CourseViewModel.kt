package com.erns.alertauni.screen.course

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erns.alertauni.data.model.StudentEnrollment
import com.erns.alertauni.data.repository.CourseRepository
import com.erns.alertauni.data.repository.StudentRepository
import com.erns.alertauni.domain.manager.DataStoreHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CourseViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val studentRepository: StudentRepository,
    private val dataStoreHelper: DataStoreHelper
) : ViewModel() {
    private val TAG = "CourseViewModel"

    private val _uiState = MutableStateFlow(CourseUiState.INPUT_CODE)
    val uiState: StateFlow<CourseUiState> = _uiState.asStateFlow()

    private val _studentEnrollmentList = MutableStateFlow<List<StudentEnrollment>>(emptyList())
    val studentEnrollmentList: StateFlow<List<StudentEnrollment>> = _studentEnrollmentList.asStateFlow()

    private val _studentEnrollment = MutableStateFlow<StudentEnrollment?>(null)
    val studentEnrollment: StateFlow<StudentEnrollment?> = _studentEnrollment.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _username = MutableStateFlow("Estudiante")
    val username: StateFlow<String> = _username.asStateFlow()

    init {
        loadUserInfo()
        getCourses()
    }

    private fun loadUserInfo() {
        viewModelScope.launch {
            val firstname = dataStoreHelper.getFirstname()
            val surname = dataStoreHelper.getSurname()
            val fullName = "$firstname $surname".trim()
            _username.value = if (fullName.isNotBlank()) fullName else "Estudiante"
        }
    }

    private fun getCourses() {
        viewModelScope.launch {
            studentRepository.getStudentEnrollment()
                .onSuccess {
                    _studentEnrollmentList.value = it
                }.onFailure {
                    Log.d(TAG, it.message.toString())
                }
        }
    }

    fun findCourseByCode(code: String) {
        if (code.isBlank()) return
        _uiState.value = CourseUiState.LOADING_QR
        _errorMessage.value = null
        viewModelScope.launch {
            val studentId = dataStoreHelper.getUserUid()
            val result = courseRepository.getCourseByCode(code)
            result.onSuccess { course ->
                _studentEnrollment.value = course
                val targetCourseId = if (course.course_catalog_id.isNotBlank()) course.course_catalog_id else course.courseId
                val isEnrolled = courseRepository.checkIsAlreadyEnrolled(studentId, targetCourseId)
                if (isEnrolled) {
                    _uiState.value = CourseUiState.ALREADY_ENROLLED
                } else {
                    _uiState.value = CourseUiState.CONFIRM_COURSE
                }
            }.onFailure { exception ->
                _studentEnrollment.value = null
                _uiState.value = CourseUiState.INPUT_CODE
                val msg = exception.message.orEmpty()
                if (msg.contains("Network", ignoreCase = true) || msg.contains("connection", ignoreCase = true) || msg.contains("Failed to connect", ignoreCase = true)) {
                    _errorMessage.value = "Error de conexión."
                } else {
                    _errorMessage.value = "El código no existe o las inscripciones están cerradas."
                }
            }
        }
    }

    fun processQrScanResult(qrCodeContent: String) {
        if (qrCodeContent.isBlank()) return
        _uiState.value = CourseUiState.FOUND_QR
        _errorMessage.value = null
        viewModelScope.launch {
            delay(1000)
            findCourseByCode(qrCodeContent)
        }
    }

    fun enrollStudent(courseId: String) {
        viewModelScope.launch {
            val studentId = dataStoreHelper.getUserUid()
            val targetId = courseId.ifBlank {
                _studentEnrollment.value?.course_catalog_id?.ifBlank { _studentEnrollment.value?.courseId } ?: ""
            }
            courseRepository.enrollStudentInCourse(studentId, targetId)
                .onSuccess {
                    _uiState.value = CourseUiState.SUCCESS
                    getCourses()
                }.onFailure {
                    _errorMessage.value = "Error de conexión."
                    _uiState.value = CourseUiState.INPUT_CODE
                }
        }
    }

    fun resetToManualInput() {
        _uiState.value = CourseUiState.INPUT_CODE
        _errorMessage.value = null
    }

    fun openQrScanner() {
        _uiState.value = CourseUiState.SCAN_QR
        _errorMessage.value = null
    }

    fun findCourse(classCode: String) {
        findCourseByCode(classCode)
    }

    fun courseEnroll(courseCatalogId: String) {
        enrollStudent(courseCatalogId)
    }

    fun clearEnrollment() {
        _studentEnrollment.value = null
        _errorMessage.value = null
        _uiState.value = CourseUiState.INPUT_CODE
    }
}