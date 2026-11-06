package com.alosir.card.domain

/**
 * 卡号「每 4 位分组显示」的光标偏移映射。
 * 组边界（每 4 位）必须落在该组末尾，不能映射到组间空格之后——
 * 否则第 4/8/12/16 位或粘贴完整卡号时偏移越界崩溃。
 */
fun cardNumberOriginalToTransformed(offset: Int): Int =
    if (offset == 0) 0 else offset + (offset - 1) / 4

fun cardNumberTransformedToOriginal(offset: Int): Int = offset - offset / 5
