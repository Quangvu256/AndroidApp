package com.example.androidapp.ui.components.forms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.ui.screens.search.SearchEvent

/**
 * Component thanh tìm kiếm phi trạng thái (stateless).
 * Hỗ trợ nhập liệu, nút xóa (clear button) và hiển thị danh sách tìm kiếm gần đây.
 */
@Composable
fun QuizSearchBar(
    query: String,
    recentSearches: List<String>,
    onEvent: (SearchEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = { onEvent(SearchEvent.OnQueryChange(it)) }, // Đã sửa tên Event
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text(stringResource(R.string.search_placeholder)) }, // Đã sửa tên String
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.search_icon_cd) // Đã sửa tên String
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onEvent(SearchEvent.OnClearSearch) }) { // Đã sửa tên Event
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.search_clear_cd) // Đã sửa tên String
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    if (query.isNotBlank()) {
                        onEvent(SearchEvent.OnSearchClicked(query)) // Đã sửa tên Event
                    }
                }
            ),
            singleLine = true,
            shape = MaterialTheme.shapes.extraLarge,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        // Hiển thị lịch sử tìm kiếm khi thanh tìm kiếm trống
        if (query.isEmpty() && recentSearches.isNotEmpty()) {
            RecentSearchesList(
                recentSearches = recentSearches,
                onRecentClick = { onEvent(SearchEvent.OnRecentSearchClicked(it)) }, // Đã sửa tên Event
                onClearAllClick = { onEvent(SearchEvent.OnClearRecentSearches) }    // Đã sửa tên Event
            )
        }
    }
}

@Composable
private fun RecentSearchesList(
    recentSearches: List<String>,
    onRecentClick: (String) -> Unit,
    onClearAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.search_recent_title), // Đã sửa tên String
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onClearAllClick) {
                Text(stringResource(R.string.search_clear_all)) // Đã sửa tên String
            }
        }
        LazyColumn {
            items(recentSearches) { recentQuery ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRecentClick(recentQuery) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = recentQuery,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}