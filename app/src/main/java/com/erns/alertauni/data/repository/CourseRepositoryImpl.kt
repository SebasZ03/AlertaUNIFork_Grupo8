package com.erns.alertauni.data.repository

import android.util.Log
import com.erns.alertauni.common.AppEvent
import com.erns.alertauni.common.AppEventManager
import com.erns.alertauni.data.model.BackendException
import com.erns.alertauni.data.model.CourseCatalogEntity
import com.erns.alertauni.data.model.StudentEnrollment
import com.erns.alertauni.data.remote.CourseDataSource
import com.erns.alertauni.data.remote.StudentDataSource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject

class CourseRepositoryImpl @Inject constructor(
    private val courseDataSource: CourseDataSource,
    private val studentDataSource: StudentDataSource,
    private val supabaseClient: SupabaseClient
) : CourseRepository {
    private val TAG = "CourseRepositoryImpl"

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
            val cleanCode = code.trim().uppercase()
            Log.d(TAG, "getCourseByCode: buscando código '$cleanCode'")
            val list = supabaseClient.postgrest["student_enrollment_view"]
                .select {
                    filter {
                        or {
                            eq("class_code", cleanCode)
                            eq("course_catalog_id", cleanCode)
                            eq("course_code", cleanCode)
                        }
                    }
                }
                .decodeList<StudentEnrollment>()

            if (list.isEmpty()) {
                Log.d(TAG, "getCourseByCode: curso no encontrado para '$cleanCode'")
                return Result.failure(Exception("El código no existe o las inscripciones están cerradas."))
            }

            Log.d(TAG, "getCourseByCode: curso encontrado -> ${list.first().courseName}")
            Result.success(list.first())
        } catch (ex: Exception) {
            Log.e(TAG, "Error in getCourseByCode: ${ex.message}", ex)
            Result.failure(ex)
        }
    }

    override suspend fun checkIsAlreadyEnrolled(studentId: String, courseId: String): Boolean {
        return try {
            val targetStudentId = if (studentId.isBlank()) "c5b12877-4122-43d8-b59a-129487563812" else studentId
            val cleanCourseId = courseId.trim()

            if (cleanCourseId.isBlank()) return false

            Log.d(TAG, "checkIsAlreadyEnrolled: verificando studentId='$targetStudentId' y course_catalog_id='$cleanCourseId'")

            // Se usa JsonObject para evitar el error de Kotlinx Serialization
            val list = supabaseClient.postgrest["student_enrollment"]
                .select {
                    filter {
                        eq("student_id", targetStudentId)
                        eq("course_catalog_id", cleanCourseId)
                    }
                }
                .decodeList<JsonObject>()

            Log.d(TAG, "checkIsAlreadyEnrolled: registros encontrados = ${list.size}")
            list.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Error in checkIsAlreadyEnrolled: ${e.message}", e)
            false
        }
    }

    override suspend fun enrollStudentInCourse(studentId: String, courseId: String): Result<Boolean> {
        return try {
            val targetStudentId = if (studentId.isBlank()) "c5b12877-4122-43d8-b59a-129487563812" else studentId
            val cleanCourseId = courseId.trim()
            Log.d(TAG, "enrollStudentInCourse: matriculando studentId=$targetStudentId en courseId=$cleanCourseId")

            // 1. Verificación previa explícita
            val isAlreadyEnrolled = checkIsAlreadyEnrolled(targetStudentId, cleanCourseId)
            if (isAlreadyEnrolled) {
                Log.d(TAG, "enrollStudentInCourse: El alumno ya estaba registrado previamente.")
                return Result.failure(Exception("ALREADY_ENROLLED"))
            }

            // 2. Inserción directa en Supabase (insert en lugar de upsert para forzar unicidad)
            supabaseClient.postgrest["student_enrollment"].insert(
                mapOf(
                    "student_id" to targetStudentId,
                    "course_catalog_id" to cleanCourseId
                )
            )
            Log.d(TAG, "enrollStudentInCourse: éxito")
            Result.success(true)
        } catch (ex: Exception) {
            Log.e(TAG, "Error in enrollStudentInCourse: ${ex.message}", ex)
            val msg = ex.message.orEmpty()
            if (msg.contains("violates unique constraint") || msg.contains("duplicate key") || msg.contains("409")) {
                Result.failure(Exception("ALREADY_ENROLLED"))
            } else {
                Result.failure(ex)
            }
        }
    }
}