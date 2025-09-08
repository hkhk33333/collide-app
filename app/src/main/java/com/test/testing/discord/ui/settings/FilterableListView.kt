package com.test.testing.discord.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun <T> FilterableListView(
    items: List<T>,
    searchText: String,
    onSearchTextChanged: (String) -> Unit,
    searchPlaceholder: String,
    itemContent: @Composable (T) -> Unit,
) {
    Column {
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchTextChanged,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            placeholder = { Text(searchPlaceholder) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = { onSearchTextChanged("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
        )
        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
            items(items) { item ->
                itemContent(item)
            }
        }
    }
}
