package com.example.androidapp.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.ui.components.common.TagChip

/**
 * Component hiển thị danh sách các từ khóa để lọc (Multi-select filter).
 * Cho phép cuộn ngang khi số lượng từ khóa nhiều hơn chiều rộng màn hình.
 *
 * @param tags Danh sách toàn bộ từ khóa hiện có.
 * @param selectedTags Danh sách các từ khóa đang được người dùng chọn.
 * @param onTagClick Callback được gọi khi một từ khóa bất kỳ được nhấn.
 * @param modifier Modifier tùy chỉnh giao diện từ bên ngoài.
 */
@Composable
fun TagFilterRow(
    tags: List<String>,
    selectedTags: List<String>,
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Ẩn toàn bộ component nếu không có tag nào để hiển thị
    if (tags.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.search_tags_label),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tags) { tag ->
                TagChip(
                    text = tag,
                    isSelected = selectedTags.contains(tag),
                    onClick = { onTagClick(tag) },
                    modifier = Modifier
                )
            }
        }
    }
}