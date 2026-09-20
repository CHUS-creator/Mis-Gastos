package com.misgastos.app.ui.nav

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.ui.graphics.vector.ImageVector
import com.misgastos.app.R

sealed class Screen(val route: String, @StringRes val labelRes: Int, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard", R.string.nav_dashboard, Icons.Filled.Dashboard)
    data object Expenses : Screen("expenses", R.string.nav_expenses, Icons.Filled.Wallet)
    data object Income : Screen("income", R.string.nav_income, Icons.Filled.TrendingUp)
    data object Budget : Screen("budget", R.string.nav_budget, Icons.Filled.Savings)
    data object Stats : Screen("stats", R.string.nav_stats, Icons.Filled.BarChart)
    data object Prices : Screen("prices", R.string.prices_title, Icons.Filled.CompareArrows)
    data object ScanEntry : Screen("scan", R.string.scan_title, Icons.Filled.CameraAlt)
    data object ScanCamera : Screen("scan_camera", R.string.scan_camera, Icons.Filled.CameraAlt)
    data object Review : Screen("review", R.string.review_title, Icons.Filled.CameraAlt)
    data class Detail(val transactionId: Long) : Screen("detail/$transactionId", R.string.detail_title, Icons.Filled.CameraAlt) {
        companion object {
            const val route = "detail/{transactionId}"
        }
    }
}

val screens = listOf(
    Screen.Dashboard,
    Screen.Expenses,
    Screen.Income,
    Screen.Budget,
    Screen.Stats,
    Screen.Prices,
)
