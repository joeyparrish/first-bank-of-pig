package io.github.joeyparrish.fbop.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import io.github.joeyparrish.fbop.data.model.AppMode
import io.github.joeyparrish.fbop.data.model.ThemeMode
import io.github.joeyparrish.fbop.data.repository.ConfigRepository
import io.github.joeyparrish.fbop.data.repository.FirebaseRepository
import io.github.joeyparrish.fbop.ui.screens.kid.KidHomeScreen
import io.github.joeyparrish.fbop.ui.screens.onboarding.*
import io.github.joeyparrish.fbop.ui.screens.parent.*

sealed class Screen(val route: String) {
    // Onboarding
    object ModeSelection : Screen("mode_selection")
    object ParentSetup : Screen("parent_setup")
    object CreateFamily : Screen("create_family")
    object JoinFamily : Screen("join_family")
    object KidSetup : Screen("kid_setup")

    // Parent mode
    object ParentHome : Screen("parent_home")
    object ChildDetail : Screen("child_detail/{childId}") {
        fun createRoute(childId: String) = "child_detail/$childId"
    }
    object AddChild : Screen("add_child")
    object EditChild : Screen("edit_child/{childId}") {
        fun createRoute(childId: String) = "edit_child/$childId"
    }
    object AddTransaction : Screen("add_transaction/{childId}") {
        fun createRoute(childId: String) = "add_transaction/$childId"
    }
    object EditTransaction : Screen("edit_transaction/{childId}/{transactionId}") {
        fun createRoute(childId: String, transactionId: String) =
            "edit_transaction/$childId/$transactionId"
    }
    object InviteParent : Screen("invite_parent")
    object ManageParents : Screen("manage_parents")
    object ChildQrCode : Screen("child_qr_code/{childId}") {
        fun createRoute(childId: String) = "child_qr_code/$childId"
    }
    object ManageDevices : Screen("manage_devices/{childId}") {
        fun createRoute(childId: String) = "manage_devices/$childId"
    }

    // Kid mode
    object KidHome : Screen("kid_home")
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    configRepository: ConfigRepository,
    firebaseRepository: FirebaseRepository,
    onThemeModeChanged: (ThemeMode) -> Unit
) {
    val config = configRepository.getConfig()

    val startDestination = when (config.mode) {
        AppMode.NOT_CONFIGURED -> Screen.ModeSelection.route
        AppMode.PARENT -> Screen.ParentHome.route
        AppMode.KID -> Screen.KidHome.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // =====================================================================
        // Onboarding
        // =====================================================================

        composable(Screen.ModeSelection.route) {
            ModeSelectionScreen(
                onParentMode = { navController.navigate(Screen.ParentSetup.route) },
                onKidMode = { navController.navigate(Screen.KidSetup.route) }
            )
        }

        composable(Screen.ParentSetup.route) {
            ParentSetupScreen(
                onCreateFamily = { navController.navigate(Screen.CreateFamily.route) },
                onJoinFamily = { navController.navigate(Screen.JoinFamily.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CreateFamily.route) {
            CreateFamilyScreen(
                firebaseRepository = firebaseRepository,
                configRepository = configRepository,
                onSuccess = {
                    navController.navigate(Screen.ParentHome.route) {
                        popUpTo(Screen.ModeSelection.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.JoinFamily.route) {
            JoinFamilyScreen(
                firebaseRepository = firebaseRepository,
                configRepository = configRepository,
                onSuccess = {
                    navController.navigate(Screen.ParentHome.route) {
                        popUpTo(Screen.ModeSelection.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.KidSetup.route) {
            KidSetupScreen(
                firebaseRepository = firebaseRepository,
                configRepository = configRepository,
                onSuccess = {
                    navController.navigate(Screen.KidHome.route) {
                        popUpTo(Screen.ModeSelection.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // =====================================================================
        // Parent Mode
        // =====================================================================

        composable(Screen.ParentHome.route) {
            ParentHomeScreen(
                firebaseRepository = firebaseRepository,
                configRepository = configRepository,
                onChildClick = { childId ->
                    navController.navigate(Screen.ChildDetail.createRoute(childId))
                },
                onAddChild = { navController.navigate(Screen.AddChild.route) },
                onInviteParent = { navController.navigate(Screen.InviteParent.route) },
                onManageParents = { navController.navigate(Screen.ManageParents.route) },
                onThemeModeChanged = onThemeModeChanged
            )
        }

        composable(
            route = Screen.ChildDetail.route,
            arguments = listOf(navArgument("childId") { type = NavType.StringType })
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: return@composable
            ChildDetailScreen(
                childId = childId,
                firebaseRepository = firebaseRepository,
                configRepository = configRepository,
                onBack = { navController.popBackStack() },
                onEditChild = { navController.navigate(Screen.EditChild.createRoute(childId)) },
                onAddTransaction = { navController.navigate(Screen.AddTransaction.createRoute(childId)) },
                onEditTransaction = { txId ->
                    navController.navigate(Screen.EditTransaction.createRoute(childId, txId))
                },
                onShowQrCode = { navController.navigate(Screen.ChildQrCode.createRoute(childId)) },
                onManageDevices = { navController.navigate(Screen.ManageDevices.createRoute(childId)) }
            )
        }

        composable(Screen.AddChild.route) {
            AddChildScreen(
                firebaseRepository = firebaseRepository,
                configRepository = configRepository,
                onSuccess = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditChild.route,
            arguments = listOf(navArgument("childId") { type = NavType.StringType })
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: return@composable
            EditChildScreen(
                childId = childId,
                firebaseRepository = firebaseRepository,
                configRepository = configRepository,
                onSuccess = { navController.popBackStack() },
                onDelete = {
                    navController.popBackStack(Screen.ParentHome.route, inclusive = false)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AddTransaction.route,
            arguments = listOf(navArgument("childId") { type = NavType.StringType })
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: return@composable
            AddTransactionScreen(
                childId = childId,
                firebaseRepository = firebaseRepository,
                configRepository = configRepository,
                onSuccess = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditTransaction.route,
            arguments = listOf(
                navArgument("childId") { type = NavType.StringType },
                navArgument("transactionId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: return@composable
            val transactionId = backStackEntry.arguments?.getString("transactionId") ?: return@composable
            EditTransactionScreen(
                childId = childId,
                transactionId = transactionId,
                firebaseRepository = firebaseRepository,
                configRepository = configRepository,
                onSuccess = { navController.popBackStack() },
                onDelete = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.InviteParent.route) {
            InviteParentScreen(
                firebaseRepository = firebaseRepository,
                configRepository = configRepository,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ManageParents.route) {
            ManageParentsScreen(
                firebaseRepository = firebaseRepository,
                configRepository = configRepository,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ChildQrCode.route,
            arguments = listOf(navArgument("childId") { type = NavType.StringType })
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: return@composable
            ChildQrCodeScreen(
                childId = childId,
                firebaseRepository = firebaseRepository,
                configRepository = configRepository,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ManageDevices.route,
            arguments = listOf(navArgument("childId") { type = NavType.StringType })
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: return@composable
            ManageDevicesScreen(
                childId = childId,
                firebaseRepository = firebaseRepository,
                configRepository = configRepository,
                onBack = { navController.popBackStack() }
            )
        }

        // =====================================================================
        // Kid Mode
        // =====================================================================

        composable(Screen.KidHome.route) {
            KidHomeScreen(
                firebaseRepository = firebaseRepository,
                configRepository = configRepository,
                onThemeModeChanged = onThemeModeChanged,
                onAccessRevoked = {
                    navController.navigate(Screen.ModeSelection.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
