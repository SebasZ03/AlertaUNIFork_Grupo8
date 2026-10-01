package com.erns.alertauni.data.repository

import com.erns.alertauni.data.model.CourseCatalogEntity
import com.erns.alertauni.data.model.StudentEnrollment

interface CourseRepository {
    suspend fun getCourseCatalogList(): Result<List<CourseCatalogEntity>>
    suspend fun getCourseByCode(code: String): Result<StudentEnrollment>
    suspend fun checkIsAlreadyEnrolled(studentId: String, courseId: String): Boolean
    suspend fun enrollStudentInCourse(studentId: String, courseId: String): Result<Boolean>
}