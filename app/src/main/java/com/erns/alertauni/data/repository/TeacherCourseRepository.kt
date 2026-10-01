package com.erns.alertauni.data.repository

import android.util.Log
import com.erns.alertauni.data.model.CourseCatalogEntity
import com.erns.alertauni.data.remote.CourseDataSource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

interface TeacherCourseRepository {
    suspend fun updateCourseEnrollmentCode(courseId: String, newCode: String): Result<Boolean>
    suspend fun getCourseEnrollmentCode(courseId: String): Result<String?>
    suspend fun getTeacherCourses(): Result<List<CourseCatalogEntity>>
}

@Serializable
data class CodeUpdatePayload(
    @SerialName("class_code") val classCode: String,
    @SerialName("class_code_enable") val classCodeEnable: Boolean = true
)

class TeacherCourseRepositoryImpl @Inject constructor(
    private val courseDataSource: CourseDataSource,
    private val supabaseClient: SupabaseClient
) : TeacherCourseRepository {

    override suspend fun updateCourseEnrollmentCode(courseId: String, newCode: String): Result<Boolean> {
        return try {
            supabaseClient.postgrest["course_catalog"].update(
                CodeUpdatePayload(classCode = newCode, classCodeEnable = true)
            ) {
                filter {
                    eq("course_catalog_id", courseId)
                }
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCourseEnrollmentCode(courseId: String): Result<String?> {
        return try {
            val cleanCourseId = courseId.trim()
            val list = supabaseClient.postgrest["course_catalog"]
                .select {
                    filter {
                        eq("course_catalog_id", cleanCourseId)
                    }
                }
                .decodeList<Map<String, Any>>()

            val row = list.firstOrNull()
            val code = row?.get("class_code")?.toString()
            Result.success(code)
        } catch (e: Exception) {
            Log.e("TeacherCourseRepository", "Error obteniendo código de Supabase: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getTeacherCourses(): Result<List<CourseCatalogEntity>> {
        return try {
            val response = courseDataSource.callGetCoursesEndpoint()
            Result.success(response.data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
