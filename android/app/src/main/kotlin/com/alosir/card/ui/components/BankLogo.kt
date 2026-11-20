package com.alosir.card.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alosir.card.R

object BankLogos {
    private val primary = listOf(
        "工商银行" to R.drawable.bank_icbc,
        "农业银行" to R.drawable.bank_abc,
        "中国银行" to R.drawable.bank_boc,
        "建设银行" to R.drawable.bank_ccb,
        "交通银行" to R.drawable.bank_bocom,
        "邮储银行" to R.drawable.bank_psbc,
        "招商银行" to R.drawable.bank_cmb,
        "浦发银行" to R.drawable.bank_spdb,
        "中信银行" to R.drawable.bank_citic,
        "光大银行" to R.drawable.bank_ceb,
        "民生银行" to R.drawable.bank_cmbc,
        "兴业银行" to R.drawable.bank_cib,
        "广发银行" to R.drawable.bank_gdb,
        "平安银行" to R.drawable.bank_pingan,
        "华夏银行" to R.drawable.bank_hxb,
        "浙商银行" to R.drawable.bank_czbank,
        "北京银行" to R.drawable.bank_bob,
        "上海银行" to R.drawable.bank_bosh,
        "南京银行" to R.drawable.bank_njcb,
        "宁波银行" to R.drawable.bank_nbcb,
        "渤海银行" to R.drawable.bank_bhbank,
        "杭州银行" to R.drawable.bank_hzbank,
        "民泰银行" to R.drawable.bank_mintai,
        "恒丰银行" to R.drawable.bank_hengfeng,
        "花旗银行" to R.drawable.bank_citi,
    )

    private val aliases = listOf(
        "邮政储蓄" to R.drawable.bank_psbc,
        "上海浦东发展银行" to R.drawable.bank_spdb,
        "浙江民泰银行" to R.drawable.bank_mintai,
        "花旗中国" to R.drawable.bank_citi,
    )

    private val byBank = (primary + aliases).toMap()

    val builtInBanks: List<Pair<String, Int>> = primary

    fun drawableFor(bankName: String?): Int? {
        val name = bankName?.trim()?.removePrefix("中国")?.takeIf { it.isNotEmpty() } ?: return null
        return byBank[name]
    }
}

@Composable
fun BankLogo(
    bankName: String?,
    glyph: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    logoKey: String? = null,
) {
    if (logoKey != null) {
        GroupLogo(bankName, logoKey, glyph, color, modifier, size)
        return
    }
    val res = BankLogos.drawableFor(bankName)
    Image(
        painter = painterResource(res ?: R.drawable.bank_placeholder),
        contentDescription = bankName ?: "未知银行",
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size).clip(RoundedCornerShape(9.dp)),
    )
}
