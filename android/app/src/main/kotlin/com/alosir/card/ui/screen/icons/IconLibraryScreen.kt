package com.alosir.card.ui.screen.icons

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alosir.card.data.icon.CustomIcon
import com.alosir.card.ui.common.PageScaffold
import com.alosir.card.ui.components.BankLogos
import com.alosir.card.ui.theme.AloCardTheme

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun IconLibraryScreen(
    selectionMode: Boolean,
    onBack: () -> Unit,
    viewModel: IconLibraryViewModel = hiltViewModel(),
) {
    var tab by remember { mutableStateOf(0) }
    var cropUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var recropIcon by remember { mutableStateOf<CustomIcon?>(null) }
    var bankDialog by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var actionIcon by remember { mutableStateOf<CustomIcon?>(null) }
    var deleteIcon by remember { mutableStateOf<CustomIcon?>(null) }
    val icons by viewModel.icons.collectAsStateWithLifecycle()
    val colors = AloCardTheme.colors
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent(),
    ) { uri -> if (uri != null) cropUri = uri }

    fun select(key: String) {
        if (selectionMode) {
            IconSelectionBus.selectedLogoKey = key
            onBack()
        }
    }

    if (cropUri != null) {
        IconCropScreen(
            uri = cropUri,
            onCancel = { cropUri = null },
            onSave = { x, y, zoom ->
                viewModel.importIcon(cropUri!!, x, y, zoom) { cropUri = null }
            },
        )
        return
    }
    if (recropIcon != null) {
        val icon = recropIcon!!
        IconCropScreen(
            originalPath = icon.originalPath,
            initialCenterX = icon.centerX,
            initialCenterY = icon.centerY,
            initialZoom = icon.zoom,
            onCancel = { recropIcon = null },
            onSave = { x, y, zoom ->
                viewModel.updateCrop(icon.id, x, y, zoom) { recropIcon = null }
            },
        )
        return
    }

    PageScaffold(
        title = "图标库",
        onBack = onBack,
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { },
                    label = { Text("银行") },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { },
                    label = { Text("自定义") },
                )
            }
        },
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (tab == 0) {
                items(BankLogos.builtInBanks, key = { it.first }) { (bank, res) ->
                    Box(
                        Modifier
                            .padding(12.dp)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.paperSunken)
                            .clickable { bankDialog = bank to res },
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(res),
                            contentDescription = bank,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(0.72f),
                        )
                    }
                }
            } else {
                item(key = "add") {
                    Box(
                        Modifier
                            .padding(12.dp)
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .border(1.dp, colors.gold, CircleShape)
                            .clickable { picker.launch("image/*") },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("+", style = androidx.compose.material3.MaterialTheme.typography.headlineLarge, color = colors.goldText)
                    }
                }
                items(icons, key = { it.id }) { icon ->
                    Box(
                        Modifier
                            .padding(12.dp)
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .combinedClickable(
                                onClick = { select("custom:${icon.id}") },
                                onLongClick = { actionIcon = icon },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        CustomIconImage(icon.croppedPath, icon.id)
                    }
                }
            }
        }
    }

    bankDialog?.let { (bank, _) ->
        AlertDialog(
            onDismissRequest = { bankDialog = null },
            text = { Text(bank) },
            confirmButton = {
                if (selectionMode) {
                    TextButton(onClick = { select("bank:$bank") }) { Text("使用此图标") }
                } else {
                    TextButton(onClick = { bankDialog = null }) { Text("关闭") }
                }
            },
        )
    }
    actionIcon?.let { icon ->
        AlertDialog(
            onDismissRequest = { actionIcon = null },
            text = { Text("选择操作") },
            confirmButton = {
                TextButton(onClick = {
                    recropIcon = icon
                    actionIcon = null
                }) { Text("裁剪") }
            },
            dismissButton = {
                TextButton(onClick = {
                    deleteIcon = icon
                    actionIcon = null
                }) { Text("删除") }
            },
        )
    }
    deleteIcon?.let { icon ->
        AlertDialog(
            onDismissRequest = { deleteIcon = null },
            title = { Text("删除图标") },
            text = { Text("删除后无法恢复，账单组将回退为默认图标。确认删除？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteIcon(icon.id)
                    deleteIcon = null
                }) { Text("确认删除") }
            },
            dismissButton = {
                TextButton(onClick = { deleteIcon = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun CustomIconImage(path: String, contentDescription: String) {
    val bitmap by produceState<ImageBitmap?>(null, path) {
        value = BitmapFactory.decodeFile(path)?.asImageBitmap()
    }
    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
