package com.erns.alertauni.screen.login

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.erns.alertauni.data.model.FCMTokenMessage
import com.erns.alertauni.data.model.GoogleAccountRequest
import com.erns.alertauni.data.repository.AuthRepository
import com.erns.alertauni.domain.manager.DataStoreHelper
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val application: Application,
    private val authRepository: AuthRepository,
    private val dataStoreHelper: DataStoreHelper,
    private val supabaseClient: SupabaseClient
) : AndroidViewModel(application) {
    private val TAG = "LoginViewModel"

    sealed class AuthState {
        object Loading : AuthState()
        object Unauthenticated : AuthState()
        object IncompleteProfile : AuthState() // Necesita elegir Rol
        object CompleteProfile : AuthState() // Necesita elegir Rol
        object Authenticated : AuthState()     // Flujo completo
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState
    private val _googleAccountRequest = MutableStateFlow<GoogleAccountRequest?>(null)
    val googleAccountRequest: StateFlow<GoogleAccountRequest?> = _googleAccountRequest.asStateFlow()
    private val _rememberMe = MutableStateFlow(false)
    val rememberMe: StateFlow<Boolean> = _rememberMe.asStateFlow()

    val userTypeMenu = mapOf(
        "PROFESSOR" to "Docente",
        "STUDENT" to "Estudiante"
    )

    init {
        // Mantiene o limpia estado inicial si es necesario
    }

    fun loginAsGuestStudent() {
        viewModelScope.launch {
            val demoStudentId = "c5b12877-4122-43d8-b59a-129487563812"
            dataStoreHelper.saveUserUid(demoStudentId)
            dataStoreHelper.saveEmail("estudiante@unsa.edu.pe")
            dataStoreHelper.setFirstname("Juan")
            dataStoreHelper.setSurname("Pérez")
            dataStoreHelper.setUserType("STUDENT")

            try {
                supabaseClient.postgrest["student"].upsert(
                    mapOf(
                        "student_id" to demoStudentId,
                        "firstname" to "Juan",
                        "surname1" to "Pérez",
                        "email" to "estudiante@unsa.edu.pe"
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error upserting demo student: $e")
            }

            _authState.value = AuthState.Authenticated
        }
    }

    fun loginAsGuestTeacher() {
        viewModelScope.launch {
            val demoProfessorId = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"
            dataStoreHelper.saveUserUid(demoProfessorId)
            dataStoreHelper.saveEmail("docente@unsa.edu.pe")
            dataStoreHelper.setFirstname("Julio")
            dataStoreHelper.setSurname("Pérez")
            dataStoreHelper.setUserType("PROFESSOR")

            try {
                supabaseClient.postgrest["professor"].upsert(
                    mapOf(
                        "professor_id" to demoProfessorId,
                        "firstname" to "Julio",
                        "surname1" to "Pérez",
                        "email" to "docente@unsa.edu.pe"
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error upserting demo professor: $e")
            }

            _authState.value = AuthState.Authenticated
        }
    }

    fun startSessionWithGoogle(context: Context) {
        Log.d(TAG, "Iniciando sesión con Google...")
        val credentialManager = CredentialManager.create(context)

        // 1. Configurar las opciones de Google ID
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false) // <-- ¡Cámbialo a FALSE para que no falle en el emulador!
            .setServerClientId("12345678-abc.apps.googleusercontent.com")
            .setAutoSelectEnabled(false) // Evita que parpadee seleccionando una cuenta automáticamente si hay varias
            .build()

        // 2. Crear la solicitud general
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        // 3. Ejecutar la solicitud dentro de una corrutina
        viewModelScope.launch {
            try {
                val result = credentialManager.getCredential(
                    context = context,
                    request = request
                )

                // 4. Extraer la credencial de Google
                val googleIdTokenCredential =
                    GoogleIdTokenCredential.createFrom(result.credential.data)

                val googleIdToken = googleIdTokenCredential.idToken
                val firstname = googleIdTokenCredential.givenName ?: ""
                val surname = googleIdTokenCredential.familyName ?: ""
                Log.d(TAG, googleIdToken)
                Log.d(TAG, googleIdTokenCredential.familyName.toString())
                Log.d(TAG, googleIdTokenCredential.givenName.toString())
                Log.d(TAG, googleIdTokenCredential.id)

                authRepository.signInSupaWithGoogle(googleIdToken).onSuccess { userInfo ->
                    val userId = userInfo.id
                    val email = userInfo.email ?: ""
                    dataStoreHelper.saveUserUid(userId)
                    dataStoreHelper.saveEmail(email)
                    dataStoreHelper.setFirstname(firstname)
                    dataStoreHelper.setSurname(surname)

                    authRepository.checkProfile().onSuccess { profile ->
                        Log.d(TAG, "Success check profile user_email: ${profile.user_email}")
                        Log.d(TAG, "Success check profile user_type: ${profile.user_type}")

                        dataStoreHelper.setUserType(profile.user_type)

                        try {
                            if (profile.user_type == "STUDENT") {
                                supabaseClient.postgrest["student"].upsert(
                                    mapOf(
                                        "student_id" to userId,
                                        "firstname" to firstname,
                                        "surname1" to surname,
                                        "email" to email
                                    )
                                )
                            } else {
                                supabaseClient.postgrest["professor"].upsert(
                                    mapOf(
                                        "professor_id" to userId,
                                        "firstname" to firstname,
                                        "surname1" to surname,
                                        "email" to email
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error upserting user profile: $e")
                        }

                        _authState.value = AuthState.Authenticated

                    }.onFailure {
                        Log.e(TAG, "Fail check profile: ${it.message}")
                        _authState.value = AuthState.IncompleteProfile
                    }

                }.onFailure { e ->
                    Log.e(TAG, "Error signing in with Google $e")
                    _authState.value = AuthState.Unauthenticated
                }

            } catch (e: NoCredentialException) {
                Log.e(TAG, "No credential found", e)
                _authState.value = AuthState.Unauthenticated
            } catch (e: GetCredentialException) {
                e.printStackTrace()
            }
        }
    }

    fun getUserInfo() {
        viewModelScope.launch {
            val googleAccountRequest = GoogleAccountRequest(
                email = dataStoreHelper.getEmail(),
                firstname = dataStoreHelper.getFirstname(),
                surname = dataStoreHelper.getSurname(),
                user_type = dataStoreHelper.getUserType()
            )
            _googleAccountRequest.value = googleAccountRequest
        }

    }

    fun updateUserProfile(googleAccountRequest: GoogleAccountRequest) {
        viewModelScope.launch {
            authRepository.updateProfile(googleAccountRequest).onSuccess {
                Log.d(TAG, "Se actualizo correctamente ${it}")
                _authState.value = AuthState.CompleteProfile
            }.onFailure {
                Log.d(TAG, "Fallo actualizando el perfil ${it}")
                _authState.value = AuthState.IncompleteProfile
            }
        }
    }

    private fun checkSession2() {
        viewModelScope.launch {
            val remember = dataStoreHelper.getRememberMe()
            _rememberMe.value = remember
        }
    }

    fun setRememberMe(value: Boolean) {
        _rememberMe.value = value
        viewModelScope.launch(Dispatchers.IO) {
            dataStoreHelper.saveRememberMe(value)
        }
    }


    private suspend fun updateFCMToken() {
        withContext(Dispatchers.IO) {
            try {
                val fcmToken = dataStoreHelper.getFCMToken()
                val payload = FCMTokenMessage(fcmToken = fcmToken)
            } catch (e: Exception) {
                Log.e("FCMTokenUpdate", "Error updating FCM token: ${e.message}")
            }
        }
    }

}
