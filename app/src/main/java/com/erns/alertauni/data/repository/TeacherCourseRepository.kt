package com.erns.alertauni.data.repository

import com.erns.alertauni.data.model.CourseCatalogEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface TeacherCourseRepository {
    suspend fun updateCourseEnrollmentCode(courseId: String, newCode: String): Result<Boolean>
    suspend fun getCourseEnrollmentCode(courseId: String): Result<String?>
    suspend fun getCourseDetails(courseCatalogId: String): Result<CourseCatalogDetails>
    suspend fun getTeacherCourses(): Result<List<CourseCatalogEntity>>
}

@Serializable
data class CourseCatalogDetails(
    @SerialName("course_catalog_id") val courseCatalogId: String = "",
    @SerialName("course_id") val courseId: String = "",
    @SerialName("course_code") val courseCode: String = "",
    @SerialName("course_name") val courseName: String = "",
    @SerialName("semester") val semester: String = "",
    @SerialName("class_code") val classCode: String = ""
)

@Serializable
data class CodeUpdatePayload(
    @SerialName("class_code") val classCode: String,
    @SerialName("class_code_enable") val classCodeEnable: Boolean = true
)
