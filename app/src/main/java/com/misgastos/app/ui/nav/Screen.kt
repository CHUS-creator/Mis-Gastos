package com.misgastos.app.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard", "Inicio", Icons.Filled.Dashboard)
    data object Expenses : Screen("expenses", "Gastos", Icons.Filled.Wallet)
    data object Income : Screen("income", "Ingresos", Icons.Filled.Savings)
    data object Budget : Screen("budget", "Presupuestos", Icons.Filled.Savings)
    data object Stats : Screen("stats", "Estadísticas", Icons.Filled.BarChart)
}

val screens = listOf(
    Screen.Dashboard,
    Screen.Expenses,
    Screen.Income,
    Screen.Budget,
    Screen.Stats,
)
