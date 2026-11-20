package com.alosir.card.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alosir.card.R
import com.alosir.card.data.icon.IconRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface IconRepositoryEntryPoint {
    fun iconRepository(): IconRepository
}

@Composable
fun GroupLogo(
    bankName: String?,
    logoKey: String?,
    glyph: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
) {
    if (logoKey?.startsWith("custom:") == true) {
        val context = LocalContext.current
        val repository = remember {
            EntryPointAccessors.fromApplication(context, IconRepositoryEntryPoint::class.java).iconRepository()
        }
        val bitmap by produceState<ImageBitmap?>(null, logoKey) {
            value = repository.getIcon(logoKey.removePrefix("custom:"))?.let {
                BitmapFactory.decodeFile(it.croppedPath)?.asImageBitmap()
            }
        }
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = bankName ?: "自定义图标",
                contentScale = ContentScale.Fit,
                modifier = modifier.size(size).clip(RoundedCornerShape(9.dp)),
            )
            return
        }
    }
    val resolvedBank = if (logoKey?.startsWith("bank:") == true) {
        logoKey.removePrefix("bank:")
    } else {
        bankName
    }
    val res = BankLogos.drawableFor(resolvedBank) ?: R.drawable.bank_placeholder
    Image(
        painter = androidx.compose.ui.res.painterResource(res),
        contentDescription = resolvedBank ?: "未知银行",
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size).clip(RoundedCornerShape(9.dp)),
    )
}
