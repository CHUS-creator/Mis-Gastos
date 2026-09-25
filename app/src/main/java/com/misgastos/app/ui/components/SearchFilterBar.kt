package com.misgastos.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.misgastos.app.R
import com.misgastos.app.util.CategoryKey
import com.misgastos.app.util.DateRangeFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedCategory: CategoryKey?,
    onCategoryChange: (CategoryKey?) -> Unit,
    categories: List<CategoryKey>,
    modifier: Modifier = Modifier,
    merchants: List<String> = emptyList(),
    selectedMerchant: String? = null,
    onMerchantChange: (String?) -> Unit = {},
    periodFilter: DateRangeFilter = DateRangeFilter.ALL,
    onPeriodChange: (DateRangeFilter) -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.action_clear_filters))
                    }
                }
            },
            singleLine = true,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CategoryDropdown(
                selectedCategory = selectedCategory,
                onCategoryChange = onCategoryChange,
                categories = categories,
                modifier = Modifier.weight(1f),
            )

            PeriodDropdown(
                selectedPeriod = periodFilter,
                onPeriodChange = onPeriodChange,
                modifier = Modifier.weight(1f),
            )
        }

        if (merchants.isNotEmpty()) {
            MerchantDropdown(
                merchants = merchants,
                selectedMerchant = selectedMerchant,
                onMerchantChange = onMerchantChange,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        val hasActiveFilters = selectedCategory != null ||
            selectedMerchant != null ||
            periodFilter != DateRangeFilter.ALL
        if (hasActiveFilters) {
            Text(
                text = stringResource(R.string.action_clear_filters),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .clickable {
                        onCategoryChange(null)
                        onMerchantChange(null)
                        onPeriodChange(DateRangeFilter.ALL)
                    },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    selectedCategory: CategoryKey?,
    onCategoryChange: (CategoryKey?) -> Unit,
    categories: List<CategoryKey>,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(value = false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selectedCategory?.let { stringResource(it.labelRes) }
                ?: stringResource(R.string.action_all_categories),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.action_filter_by_category)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_all_categories)) },
                onClick = {
                    onCategoryChange(null)
                    expanded = false
                },
            )
            categories.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.labelRes)) },
                    onClick = {
                        onCategoryChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodDropdown(
    selectedPeriod: DateRangeFilter,
    onPeriodChange: (DateRangeFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(value = false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = stringResource(selectedPeriod.labelRes),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.filter_period_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DateRangeFilter.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.labelRes)) },
                    onClick = {
                        onPeriodChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MerchantDropdown(
    merchants: List<String>,
    selectedMerchant: String?,
    onMerchantChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(value = false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selectedMerchant ?: stringResource(R.string.filter_merchant_all),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.filter_merchant_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.filter_merchant_all)) },
                onClick = {
                    onMerchantChange(null)
                    expanded = false
                },
            )
            merchants.forEach { merchant ->
                DropdownMenuItem(
                    text = { Text(merchant) },
                    onClick = {
                        onMerchantChange(merchant)
                        expanded = false
                    },
                )
            }
        }
    }
}
