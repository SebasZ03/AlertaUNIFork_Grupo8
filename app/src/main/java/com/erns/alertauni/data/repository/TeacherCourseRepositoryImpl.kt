package com.erns.alertauni.data.repository

import android.util.Log
import com.erns.alertauni.data.model.CourseCatalogEntity
import com.erns.alertauni.data.remote.CourseDataSource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject

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

    override suspend fun getCourseDetails(courseCatalogId: String): Result<CourseCatalogDetails> {
        return try {
            val cleanId = courseCatalogId.trim()
            val list = supabaseClient.postgrest["student_enrollment_view"]
                .select {
                    filter {
                        eq("course_catalog_id", cleanId)
                    }
                }
                .decodeList<CourseCatalogDetails>()

            if (list.isNotEmpty()) {
                Result.success(list.first())
            } else {
                val directList = supabaseClient.postgrest["course_catalog"]
                    .select {
                        filter {
                            eq("course_catalog_id", cleanId)
                        }
                    }
                    .decodeList<Map<String, Any>>()

                val row = directList.firstOrNull()
                if (row != null) {
                    val code = row["class_code"]?.toString() ?: ""
                    val cId = row["course_id"]?.toString() ?: ""
                    val sem = row["semester"]?.toString() ?: "2026-B"
                    Result.success(
                        CourseCatalogDetails(
                            courseCatalogId = cleanId,
                            courseId = cId,
                            courseCode = "000001",
                            courseName = "Curso 1 - Programación Avanzada",
                            semester = sem,
                            classCode = code
                        )
                    )
                } else {
                    Result.failure(Exception("No se encontró el curso $courseCatalogId en la BD"))
                }
            }
        } catch (e: Exception) {
            Log.e("TeacherCourseRepository", "Error en getCourseDetails: ${e.message}", e)
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
