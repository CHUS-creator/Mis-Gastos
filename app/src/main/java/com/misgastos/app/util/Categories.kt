package com.misgastos.app.util

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.misgastos.app.R

enum class CategoryKey(val stableValue: String, @StringRes val labelRes: Int) {
    FOOD("Comida", R.string.cat_food),
    TRANSPORT("Transporte", R.string.cat_transport),
    HOUSING("Vivienda", R.string.cat_housing),
    LEISURE("Ocio", R.string.cat_leisure),
    HEALTH("Salud", R.string.cat_health),
    CLOTHING("Ropa", R.string.cat_clothing),
    SUBSCRIPTIONS("Suscripciones", R.string.cat_subscriptions),
    OTHER_EXPENSE("Otros", R.string.cat_other_expense),
    SALARY("Salario", R.string.cat_salary),
    FREELANCE("Freelance", R.string.cat_freelance),
    INVESTMENTS("Inversiones", R.string.cat_investments),
    GIFT("Regalo", R.string.cat_gift),
    OTHER_INCOME("Otros", R.string.cat_other_income),
    ;

    companion object {
        private val byValue = entries.associateBy { it.stableValue }

        fun fromValue(value: String): CategoryKey? = byValue[value]
    }
}

fun categoryLabel(context: Context, value: String): String =
    CategoryKey.fromValue(value)?.let { context.getString(it.labelRes) } ?: value

@Composable
fun categoryLabel(value: String): String =
    categoryLabel(LocalContext.current, value)

object Categories {
    val expenseCategories: List<CategoryKey> = listOf(
        CategoryKey.FOOD,
        CategoryKey.TRANSPORT,
        CategoryKey.HOUSING,
        CategoryKey.LEISURE,
        CategoryKey.HEALTH,
        CategoryKey.CLOTHING,
        CategoryKey.SUBSCRIPTIONS,
        CategoryKey.OTHER_EXPENSE,
    )

    val incomeCategories: List<CategoryKey> = listOf(
        CategoryKey.SALARY,
        CategoryKey.FREELANCE,
        CategoryKey.INVESTMENTS,
        CategoryKey.GIFT,
        CategoryKey.OTHER_INCOME,
    )
}
