package com.alosir.card.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_icons")
data class CustomIconEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "original_path")
    val originalPath: String,
    @ColumnInfo(name = "cropped_path")
    val croppedPath: String,
    @ColumnInfo(name = "center_x")
    val centerX: Float,
    @ColumnInfo(name = "center_y")
    val centerY: Float,
    @ColumnInfo(name = "zoom")
    val zoom: Float,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
