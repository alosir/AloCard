package com.alosir.card.data.icon

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.net.Uri
import com.alosir.card.data.local.dao.CustomIconDao
import com.alosir.card.data.local.entity.CustomIconEntity
import com.alosir.card.domain.IconCropMath
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class CustomIcon(
    val id: String,
    val originalPath: String,
    val croppedPath: String,
    val centerX: Float,
    val centerY: Float,
    val zoom: Float,
)

@Singleton
class IconRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: CustomIconDao,
) {
    private val directory = File(context.filesDir, "icons").apply { mkdirs() }

    fun observeIcons(): Flow<List<CustomIcon>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    suspend fun getIcon(id: String): CustomIcon? = dao.getById(id)?.toDomain()

    suspend fun importIcon(uri: Uri, centerX: Float, centerY: Float, zoom: Float): String {
        val source = context.contentResolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it)
        } ?: throw IllegalArgumentException("无法读取图片")
        val id = UUID.randomUUID().toString()
        val original = File(directory, "$id-original.png")
        original.outputStream().use { source.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val cropped = File(directory, "$id-cropped.png")
        writeCrop(source, cropped, centerX, centerY, zoom)
        val now = System.currentTimeMillis()
        dao.insert(CustomIconEntity(id, original.absolutePath, cropped.absolutePath, centerX, centerY, zoom, now, now))
        return id
    }

    suspend fun updateCrop(id: String, centerX: Float, centerY: Float, zoom: Float) {
        val existing = dao.getById(id) ?: return
        val source = BitmapFactory.decodeFile(existing.originalPath)
            ?: throw IllegalArgumentException("原图已不可用")
        writeCrop(source, File(existing.croppedPath), centerX, centerY, zoom)
        dao.update(existing.copy(centerX = centerX, centerY = centerY, zoom = zoom, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteIcon(id: String) {
        val existing = dao.getById(id) ?: return
        dao.deleteById(id)
        File(existing.originalPath).delete()
        File(existing.croppedPath).delete()
    }

    private fun writeCrop(source: Bitmap, output: File, centerX: Float, centerY: Float, zoom: Float) {
        val region = IconCropMath.crop(source.width, source.height, centerX, centerY, zoom)
        val square = Bitmap.createBitmap(source, region.left, region.top, region.side, region.side)
        val result = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val path = Path().apply { addCircle(128f, 128f, 128f, Path.Direction.CW) }
        canvas.clipPath(path)
        canvas.drawBitmap(square, null, Rect(0, 0, 256, 256), Paint(Paint.ANTI_ALIAS_FLAG))
        output.outputStream().use { result.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun CustomIconEntity.toDomain() = CustomIcon(id, originalPath, croppedPath, centerX, centerY, zoom)
}
