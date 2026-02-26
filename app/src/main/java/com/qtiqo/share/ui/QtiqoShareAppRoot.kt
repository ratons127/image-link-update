package com.qtiqo.share.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.qtiqo.share.domain.model.UserRole
import com.qtiqo.share.feature.admin.ui.AdminNavGraph
import com.qtiqo.share.feature.profile.ui.ProfileScreen
import com.qtiqo.share.ui.navigation.Route
import com.qtiqo.share.ui.navigation.bottomRoutes
import com.qtiqo.share.ui.screens.AuthLoginScreen
import com.qtiqo.share.ui.screens.ForgotPasswordScreen
import com.qtiqo.share.ui.screens.GalleryScreen
import com.qtiqo.share.ui.screens.FileDetailScreen
import com.qtiqo.share.ui.screens.PublicViewScreen
import com.qtiqo.share.ui.screens.SignUpScreen
import com.qtiqo.share.ui.screens.UploadScreen
import com.qtiqo.share.ui.viewmodel.SessionViewModel

@Composable
fun QtiqoShareAppRoot() {
    val navController = rememberNavController()
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val session by sessionViewModel.session.collectAsStateWithLifecycle()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route
    val showBottomBar = currentBackStack?.destination?.hierarchy?.any { it.route in bottomRoutes } == true

    val startDestination = if (session == null) Route.Login.path else Route.Gallery.path

    Scaffold(
        bottomBar = {
            if (showBottomBar && session != null) {
                NavigationBar {
                    val navItems = buildList {
                        add(Route.Gallery.path to ("Gallery" to Icons.Default.Collections))
                        add(Route.Upload.path to ("Upload" to Icons.Default.FileUpload))
                        add(Route.Profile.path to ("Profile" to Icons.Default.Person))
                        if (session?.role == UserRole.ADMIN) {
                            add(Route.Admin.path to ("Admin" to Icons.Default.AdminPanelSettings))
                        }
                    }
                    navItems.forEach { (route, labelIcon) ->
                        NavigationBarItem(
                            selected = currentRoute == route,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(Route.Gallery.path) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(labelIcon.second, contentDescription = labelIcon.first) },
                            label = { Text(labelIcon.first) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Route.Login.path) {
                AuthLoginScreen(
                    onGoSignUp = { navController.navigate(Route.SignUp.path) },
                    onGoForgot = { navController.navigate(Route.Forgot.path) },
                    onSignedIn = {
                        navController.navigate(Route.Gallery.path) {
                            popUpTo(Route.Login.path) { inclusive = true }
                        }
                    }
                )
            }
            composable(Route.SignUp.path) {
                SignUpScreen(
                    onBack = { navController.popBackStack() },
                    onSignedUp = {
                        navController.navigate(Route.Gallery.path) {
                            popUpTo(Route.Login.path) { inclusive = true }
                        }
                    }
                )
            }
            composable(Route.Forgot.path) {
                ForgotPasswordScreen(onBack = { navController.popBackStack() })
            }
            composable(Route.Gallery.path) {
                GalleryScreen(
                    onUploadClick = { navController.navigate(Route.Upload.path) },
                    onOpenDetail = { navController.navigate(Route.FileDetail.create(it)) }
                )
            }
            composable(Route.Upload.path) {
                UploadScreen(onOpenDetail = { navController.navigate(Route.FileDetail.create(it)) })
            }
            composable(Route.Profile.path) {
                ProfileScreen(
                    onLoggedOut = {
                        val rootId = navController.graph.id
                        navController.navigate(Route.Login.path) {
                            popUpTo(rootId) { inclusive = true }
                        }
                    }
                )
            }
            AdminNavGraph(
                onBack = { navController.popBackStack() },
                onOpenShareLink = { shareUrl ->
                    val token = shareUrl.substringAfter("/s/", "")
                    if (token.isNotBlank()) {
                        navController.navigate(Route.Public.create(token))
                    }
                }
            )
            composable(
                route = Route.FileDetail.path,
                arguments = listOf(navArgument("fileId") { type = NavType.StringType })
            ) {
                FileDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Route.Public.path,
                arguments = listOf(navArgument("token") { type = NavType.StringType }),
                deepLinks = listOf(
                    navDeepLink { uriPattern = "https://imagelink.qtiqo.com/s/{token}" }
                )
            ) {
                PublicViewScreen(
                    onBack = { navController.popBackStack() },
                    onOpenInAppGallery = {
                        if (session != null) navController.navigate(Route.Gallery.path)
                        else navController.navigate(Route.Login.path)
                    }
                )
            }
        }
    }
}
