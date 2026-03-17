package com.example.androidapp.ui.screens.create

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.ui.components.forms.SwitchToggle
import com.example.androidapp.ui.theme.InterFamily
import com.example.androidapp.ui.theme.QuizzezTheme

/**
 * A grouped section that houses the "Công khai" and "Đóng góp câu hỏi" toggles
 * shared between [CreateQuizScreen] and [EditQuizScreen].
 *
 * ### Draft-mode constraint
 * When [isDraft] is `true` both toggles are rendered but **disabled**, and an
 * explanatory banner is shown beneath them. This prevents the user from accidentally
 * enabling publish-only options while saving a draft:
 * - Enabling either toggle in the ViewModel exits draft mode ([isDraft] → `false`),
 *   which re-enables both controls here.
 * - Pressing "Lưu nháp" resets both flags to `false` in the ViewModel regardless of
 *   their UI state, keeping what is stored and what is shown in sync.
 *
 * @param isPublic Current value of the "Công khai" toggle.
 * @param shareToPool Current value of the "Đóng góp câu hỏi" toggle.
 * @param isDraft Whether the quiz is currently in draft mode.
 *   When `true`, both toggles are disabled and a hint is displayed.
 * @param onIsPublicChanged Callback invoked when the "Công khai" switch is flipped.
 * @param onShareToPoolChanged Callback invoked when the "Đóng góp câu hỏi" switch is flipped.
 * @param modifier Modifier for layout customisation.
 */
@Composable
fun PublishOptionsSection(
    isPublic: Boolean,
    shareToPool: Boolean,
    isDraft: Boolean,
    onIsPublicChanged: (Boolean) -> Unit,
    onShareToPoolChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // ── "Công khai" row ──────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.create_quiz_public),
                    fontFamily = InterFamily,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isDraft)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }
            Switch(
                checked = isPublic,
                onCheckedChange = onIsPublicChanged,
                enabled = !isDraft
            )
        }

        // ── "Đóng góp câu hỏi" row ──────────────────────────────────────
        SwitchToggle(
            checked = shareToPool,
            onCheckedChange = onShareToPoolChanged,
            label = stringResource(R.string.create_quiz_share_to_pool),
            description = stringResource(R.string.create_quiz_share_to_pool_desc),
            enabled = !isDraft,
            modifier = Modifier.fillMaxWidth()
        )

        // ── Draft hint banner ────────────────────────────────────────────
        // Only visible while the quiz is in draft state.
        if (isDraft) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.create_publish_options_draft_hint),
                fontFamily = InterFamily,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Previews
// ──────────────────────────────────────────────────────────────────────────────

@Preview(name = "Draft mode — Light", showBackground = true)
@Composable
private fun PublishOptionsDraftLightPreview() {
    QuizzezTheme(darkTheme = false) {
        Surface {
            PublishOptionsSection(
                isPublic = false,
                shareToPool = false,
                isDraft = true,
                onIsPublicChanged = {},
                onShareToPoolChanged = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "Draft mode — Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PublishOptionsDraftDarkPreview() {
    QuizzezTheme(darkTheme = true) {
        Surface {
            PublishOptionsSection(
                isPublic = false,
                shareToPool = false,
                isDraft = true,
                onIsPublicChanged = {},
                onShareToPoolChanged = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "Publish mode — Light", showBackground = true)
@Composable
private fun PublishOptionsPublishLightPreview() {
    QuizzezTheme(darkTheme = false) {
        Surface {
            PublishOptionsSection(
                isPublic = true,
                shareToPool = true,
                isDraft = false,
                onIsPublicChanged = {},
                onShareToPoolChanged = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "Publish mode — Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PublishOptionsPublishDarkPreview() {
    QuizzezTheme(darkTheme = true) {
        Surface {
            PublishOptionsSection(
                isPublic = true,
                shareToPool = true,
                isDraft = false,
                onIsPublicChanged = {},
                onShareToPoolChanged = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
