package com.erns.alertauni.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.ui.platform.LocalContext
import com.erns.alertauni.domain.manager.DataStoreHelper
import com.erns.alertauni.screen.common.UnderDevelopmentScreen
import com.erns.alertauni.screen.contact.StudentContactScreen
import com.erns.alertauni.screen.contact.TeacherContactScreen
import com.erns.alertauni.screen.course.StudentCourseRoute
import com.erns.alertauni.screen.course.TeacherCourseDetailRoute
import com.erns.alertauni.screen.login.LoginScreen
import com.erns.alertauni.screen.login.ProfileScreen
import com.erns.alertauni.screen.post.PostCommentScreen
import com.erns.alertauni.screen.post.PostScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@Composable
fun MainAppNavigation(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val userTypeSelected = remember { mutableStateOf("") }

    if (userTypeSelected.value.isEmpty()) {
        val loginNavController = rememberNavController()
        NavHost(
            navController = loginNavController,
            startDestination = RouteScreen.Login.route
        ) {
            composable(RouteScreen.Login.route) {
                LoginScreen(
                    onIncompleteProfile = {
                        loginNavController.navigate(RouteScreen.Profile.route) {
                            popUpTo(RouteScreen.Login.route) {
                                inclusive = false
                            }
                        }
                    },
                    onLoginSuccess = { userType ->
                        userTypeSelected.value = userType
                    }
                )
            }

            composable(RouteScreen.Profile.route) {
                ProfileScreen(
                    onCompleteProfile = { userType ->
                        userTypeSelected.value = userType
                    }
                )
            }
        }
    } else {
        MainMenuScreen(
            userType = userTypeSelected.value,
            onLogout = {
                userTypeSelected.value = ""
                CoroutineScope(Dispatchers.IO).launch {
                    DataStoreHelper(context).clearUserType()
                    DataStoreHelper(context).clearUserUid()
                }
            }
        )
    }
}

@Composable
fun MainMenuScreen(userType: String, onLogout: () -> Unit) {
    val items = listOf(
        RouteMainMenu.Posts,
        RouteMainMenu.Courses,
        if (userType == "PROFESSOR") RouteMainMenu.TeacherContacts else RouteMainMenu.StudentContacts,
        RouteMainMenu.Profile
    )
    val fabAction = remember { mutableStateOf<() -> Unit>({}) }
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentBackStackEntry = navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry.value?.destination?.route

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (currentDestination == RouteScreen.Posts.route ||
                currentDestination == RouteMainMenu.Courses.route
            ) {
                FloatingActionButton(
                    shape = CircleShape,
                    onClick = fabAction.value,
                    containerColor = Color(0xFFC59A27),
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                val currentDestination = navController.currentBackStackEntryAsState().value?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        selected = currentDestination?.route == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFC59A27),
                            selectedTextColor = Color(0xFFC59A27),
                            indicatorColor = Color.Transparent,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = RouteMainMenu.Posts.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(RouteMainMenu.Posts.route) {
                PostScreen(
                    onClickComment = { postId ->
                        navController.navigate(RouteScreen.Comment.route + "/$postId")
                    },
                    snackbarHostState = snackbarHostState,
                    onFabActionReady = { fabAction.value = it }
                )
            }
            composable(
                route = RouteScreen.Comment.route + "/{postId}",
                arguments = listOf(
                    navArgument("postId") {
                        type = NavType.StringType
                    }
                )
            ) {
                PostCommentScreen()
            }

            composable(RouteMainMenu.Courses.route) {
                if (userType == "PROFESSOR") {
                    TeacherCourseDetailRoute(
                        onLogout = onLogout,
                        onGoToPosts = {
                            navController.navigate(RouteMainMenu.Posts.route)
                        }
                    )
                } else {
                    StudentCourseRoute(
                        onLogout = onLogout,
                        onGoToCourse = { courseId ->
                            navController.navigate(RouteMainMenu.Posts.route) {
                                popUpTo(RouteMainMenu.Posts.route) { saveState = true }
                                launchSingleTop = true
                            }
                        },
                        snackbarHostState = snackbarHostState,
                        onFabActionReady = { action -> fabAction.value = action }
                    )
                }
            }

            composable(RouteScreen.StudentCourseEnrollment.route) {
                StudentCourseRoute(
                    onLogout = onLogout,
                    onGoToCourse = { courseId ->
                        navController.navigate(RouteMainMenu.Posts.route) {
                            popUpTo(RouteMainMenu.Posts.route) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    snackbarHostState = snackbarHostState,
                    onFabActionReady = { action -> fabAction.value = action }
                )
            }

            composable(RouteMainMenu.TeacherContacts.route) {
                TeacherContactScreen()
            }
            composable(RouteMainMenu.StudentContacts.route) {
                StudentContactScreen()
            }

            composable(RouteMainMenu.Profile.route) {
                UnderDevelopmentScreen(title = "Perfil de Usuario")
            }
        }
    }
}
