package com.erns.alertauni.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class RouteMainMenu(val route: String, val label: String, val icon: ImageVector) {
    object Posts : RouteMainMenu("posts", "Inicio", Icons.Default.Home)
    object Courses : RouteMainMenu("courses", "Mis cursos", Icons.Default.DateRange)
    object StudentContacts : RouteMainMenu("StudentContacts", "Contactos", Icons.Default.Person)
    object TeacherContacts : RouteMainMenu("TeacherContacts", "Contactos", Icons.Default.Person)
    object Profile : RouteMainMenu("profile_main", "Perfil", Icons.Default.AccountBox)
}
