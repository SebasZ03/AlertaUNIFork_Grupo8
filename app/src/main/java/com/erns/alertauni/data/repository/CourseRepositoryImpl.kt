package com.erns.alertauni.data.repository

import com.erns.alertauni.common.AppEvent
import com.erns.alertauni.common.AppEventManager
import com.erns.alertauni.data.model.BackendException
import com.erns.alertauni.data.model.ClassCodeRequest
import com.erns.alertauni.data.model.CourseCatalogEntity
import com.erns.alertauni.data.model.CourseEnrollRequest
import com.erns.alertauni.data.model.StudentEnrollment
import com.erns.alertauni.data.remote.CourseDataSource
import com.erns.alertauni.data.remote.StudentDataSource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject

class CourseRepositoryImpl @Inject constructor(
    private val courseDataSource: CourseDataSource,
    private val studentDataSource: StudentDataSource,
    private val supabaseClient: SupabaseClient
) : CourseRepository {
    override suspend fun getCourseCatalogList(): Result<List<CourseCatalogEntity>> {
        return try {
            val success = courseDataSource.callGetCoursesEndpoint()
            if (success.data.isNotEmpty()) {
                Result.success(success.data)
            } else {
                Result.failure(Exception("No courses found"))
            }
        } catch (e: BackendException) {
            if (e.errorData.code == "PROFILE_NOT_FOUND") {
                AppEventManager.emit(AppEvent.NavigateToCompleteProfile)
            }
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCourseByCode(code: String): Result<StudentEnrollment> {
        return try {
            val response = studentDataSource.callFindCourseEndpoint(ClassCodeRequest(classCode = code))
            Result.success(response.data)
        } catch (e: Exception) {
            try {
                val cleanCode = code.trim().uppercase()
                val course = supabaseClient.postgrest["student_enrollment_view"]
                    .select {
                        filter {
                            or {
                                eq("class_code", cleanCode)
                                eq("course_catalog_id", cleanCode)
                                eq("course_code", cleanCode)
                            }
                        }
                    }
                    .decodeSingle<StudentEnrollment>()
                Result.success(course)
            } catch (ex: Exception) {
                Result.failure(ex)
            }
        }
    }

    override suspend fun checkIsAlreadyEnrolled(studentId: String, courseId: String): Boolean {
        return try {
            val enrollments = studentDataSource.callStudentEnrollmentEndpoint().data
            enrollments.any { enrollment ->
                enrollment.courseId == courseId ||
                        enrollment.course_catalog_id == courseId ||
                        enrollment.courseCode == courseId
            }
        } catch (e: Exception) {
            try {
                val list = supabaseClient.postgrest["student_enrollment"]
                    .select {
                        filter {
                            eq("student_id", studentId)
                            eq("course_catalog_id", courseId)
                        }
                    }
                    .decodeList<Map<String, Any>>()
                list.isNotEmpty()
            } catch (ex: Exception) {
                false
            }
        }
    }

    override suspend fun enrollStudentInCourse(studentId: String, courseId: String): Result<Boolean> {
        return try {
            studentDataSource.callCourseEnrollEndpoint(CourseEnrollRequest(courseCatalogId = courseId))
            Result.success(true)
        } catch (e: Exception) {
            try {
                supabaseClient.postgrest["student_enrollment"].insert(
                    mapOf(
                        "student_id" to studentId,
                        "course_catalog_id" to courseId
                    )
                )
                Result.success(true)
            } catch (ex: Exception) {
                Result.failure(ex)
            }
        }
    }
}
