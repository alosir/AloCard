package com.alosir.card.ui.screen.bintable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alosir.card.ui.common.FilledField
import com.alosir.card.ui.common.HintText
import com.alosir.card.ui.common.PageScaffold
import com.alosir.card.ui.theme.AloCardTheme

@Composable
fun BinTableScreen(
    onBack: () -> Unit,
    viewModel: BinTableViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val colors = AloCardTheme.colors

    PageScaffold(title = "号段表", onBack = onBack) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            FilledField(
                value = query,
                onValueChange = { viewModel.query.value = it },
                label = "搜索银行或号段",
                placeholder = "如 中国银行 / 622848",
                supportingText = "快照 ${viewModel.snapshot ?: "未知"} · ${rows.size} 条",
            )
            LazyColumn(Modifier.fillMaxSize()) {
                items(
                    rows,
                    key = { "${it.prefix}-${it.bankName}-${it.cardType}-${it.length}" },
                ) { entry ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .background(colors.paperSunken)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(entry.prefix, style = MaterialTheme.typography.labelLarge)
                        Text(
                            entry.bankName +
                                (entry.cardType?.let { " · $it" } ?: "") +
                                (entry.length?.let { " · $it 位" } ?: ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
