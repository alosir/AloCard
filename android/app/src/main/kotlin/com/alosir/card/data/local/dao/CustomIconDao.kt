package com.alosir.card.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.alosir.card.data.local.entity.CustomIconEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomIconDao {
    @Query("SELECT * FROM custom_icons ORDER BY created_at ASC")
    fun observeAll(): Flow<List<CustomIconEntity>>

    @Query("SELECT * FROM custom_icons ORDER BY created_at ASC")
    suspend fun getAllOnce(): List<CustomIconEntity>

    @Query("SELECT * FROM custom_icons WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CustomIconEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(icon: CustomIconEntity)

    @Update
    suspend fun update(icon: CustomIconEntity)

    @Query("DELETE FROM custom_icons WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM custom_icons")
    suspend fun deleteAll()
}
