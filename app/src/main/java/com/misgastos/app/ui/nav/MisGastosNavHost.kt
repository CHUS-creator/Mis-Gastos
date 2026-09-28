package com.misgastos.app.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.misgastos.app.ui.screens.budget.BudgetScreen
import com.misgastos.app.ui.screens.dashboard.DashboardScreen
import com.misgastos.app.ui.screens.detail.TransactionDetailScreen
import com.misgastos.app.ui.screens.expenses.ExpensesScreen
import com.misgastos.app.ui.screens.income.IncomeScreen
import com.misgastos.app.ui.screens.scan.OcrBenchScreen
import com.misgastos.app.ui.screens.settings.OcrSettingsScreen
import com.misgastos.app.ui.screens.scan.ReviewReceiptScreen
import com.misgastos.app.ui.screens.scan.ScanCameraScreen
import com.misgastos.app.ui.screens.scan.ScanEntryScreen
import com.misgastos.app.ui.screens.prices.PriceComparisonScreen
import com.misgastos.app.ui.screens.stats.StatsScreen
import com.misgastos.app.viewmodel.MisGastosViewModel
import com.misgastos.app.viewmodel.ScanState

@Composable
fun MisGastosNavHost() {
    val navController: NavHostController = rememberNavController()
    val viewModel: MisGastosViewModel = viewModel(factory = MisGastosViewModel.Factory)
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbar by viewModel.snackbar.collectAsState()

    LaunchedEffect(snackbar) {
        snackbar?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeSnackbar()
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in screens.map { it.route }

    val scanState by viewModel.scanState.collectAsState()
    LaunchedEffect(scanState) {
        when (scanState) {
            is ScanState.Done -> {
                navController.navigate(Screen.Review.route) {
                    launchSingleTop = true
                }
                viewModel.consumeScanState()
            }
            ScanState.Error -> {
                snackbarHostState.showSnackbar("Error")
                viewModel.consumeScanState()
            }
            else -> {}
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) NavigationBar {
                screens.forEach { screen ->
                    val label = stringResource(screen.labelRes)
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(screen.icon, contentDescription = label) },
                        label = { Text(label) },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = viewModel,
                    onScanClick = { navController.navigate(Screen.ScanEntry.route) },
                    onTransactionClick = { id -> navController.navigate("detail/$id") },
                    onSettingsClick = { navController.navigate(Screen.OcrSettings.route) },
                )
            }
            composable(Screen.Expenses.route) {
                ExpensesScreen(
                    viewModel = viewModel,
                    onTransactionClick = { id -> navController.navigate("detail/$id") },
                )
            }
            composable(Screen.Income.route) {
                IncomeScreen(
                    viewModel = viewModel,
                    onTransactionClick = { id -> navController.navigate("detail/$id") },
                )
            }
            composable(Screen.Budget.route) { BudgetScreen(viewModel) }
            composable(Screen.Stats.route) { StatsScreen(viewModel) }
            composable(Screen.Prices.route) {
                PriceComparisonScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Screen.ScanEntry.route) {
                ScanEntryScreen(
                    onCameraGranted = { navController.navigate(Screen.ScanCamera.route) },
                    onImagePicked = { uri -> viewModel.processReceipt(uri) },
                    onOcrBench = { navController.navigate(Screen.OcrBench.route) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Screen.ScanCamera.route) {
                ScanCameraScreen(
                    onCaptured = { uri -> viewModel.processReceipt(uri) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Screen.Review.route) {
                ReviewReceiptScreen(
                    viewModel = viewModel,
                    onSaved = {
                        navController.popBackStack(Screen.Dashboard.route, inclusive = false)
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Screen.OcrBench.route) {
                OcrBenchScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Screen.OcrSettings.route) {
                OcrSettingsScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Screen.Detail.route,
                arguments = listOf(navArgument("transactionId") { type = NavType.LongType }),
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("transactionId") ?: -1L
                TransactionDetailScreen(
                    viewModel = viewModel,
                    transactionId = id,
                    onBack = {
                        viewModel.clearDetail()
                        navController.popBackStack()
                    },
                    onDeleted = {
                        viewModel.clearDetail()
                        navController.popBackStack()
                    },
                )
            }
        }
    }
}
