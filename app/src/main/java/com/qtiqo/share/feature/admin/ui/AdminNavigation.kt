package com.qtiqo.share.feature.admin.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.qtiqo.share.feature.admin.integration.Session

const val ADMIN_ROUTE = "admin"

data class AdminBottomNavItem(
    val route: String = ADMIN_ROUTE,
    val label: String = "Admin",
    val icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.AdminPanelSettings
)

fun adminBottomNavItemOrNull(session: Session?): AdminBottomNavItem? =
    if (session?.role.equals("ADMIN", true)) AdminBottomNavItem() else null

fun NavGraphBuilder.AdminNavGraph(
    onBack: () -> Unit,
    onOpenShareLink: (String) -> Unit = {}
) {
    composable(ADMIN_ROUTE) {
        AdminRoute(onBack = onBack, onOpenShareLink = onOpenShareLink, viewModel = hiltViewModel())
    }
}
