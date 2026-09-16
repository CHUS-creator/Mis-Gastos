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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.misgastos.app.ui.screens.budget.BudgetScreen
import com.misgastos.app.ui.screens.dashboard.DashboardScreen
import com.misgastos.app.ui.screens.expenses.ExpensesScreen
import com.misgastos.app.ui.screens.income.IncomeScreen
import com.misgastos.app.ui.screens.stats.StatsScreen
import com.misgastos.app.viewmodel.MisGastosViewModel

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

    Scaffold(
        bottomBar = {
            NavigationBar {
                screens.forEach { screen ->
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
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) { DashboardScreen(viewModel) }
            composable(Screen.Expenses.route) { ExpensesScreen(viewModel) }
            composable(Screen.Income.route) { IncomeScreen(viewModel) }
            composable(Screen.Budget.route) { BudgetScreen(viewModel) }
            composable(Screen.Stats.route) { StatsScreen(viewModel) }
        }
    }
}
