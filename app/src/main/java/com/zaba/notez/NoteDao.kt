package com.zaba.notez

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE deletedAt IS NULL ORDER BY isPinned DESC, updatedAt DESC")
    fun observeAll(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE deletedAt IS NULL AND (title LIKE '%' || :q || '%' OR content LIKE '%' || :q || '%') ORDER BY isPinned DESC, updatedAt DESC")
    fun search(q: String): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id AND deletedAt IS NULL")
    suspend fun getById(id: Long): Note?

    @Query("SELECT * FROM notes WHERE deletedAt IS NULL ORDER BY isPinned DESC, updatedAt DESC")
    suspend fun getAllNow(): List<Note>

    @Query("SELECT * FROM notes WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeRecycle(): Flow<List<Note>>

    @Query("UPDATE notes SET deletedAt = :now WHERE id IN (:ids)")
    suspend fun moveToRecycle(ids: List<Long>, now: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET deletedAt = NULL WHERE id IN (:ids)")
    suspend fun restoreFromRecycle(ids: List<Long>)

    @Query("UPDATE notes SET isPinned = :pinned WHERE id IN (:ids)")
    suspend fun setPinned(ids: List<Long>, pinned: Boolean)

    @Query("DELETE FROM notes WHERE id IN (:ids)")
    suspend fun deletePermanently(ids: List<Long>)

    @Query("DELETE FROM notes WHERE deletedAt IS NOT NULL")
    suspend fun clearRecycle()

    @Query("DELETE FROM notes WHERE deletedAt IS NOT NULL AND deletedAt < :cutoff")
    suspend fun purgeExpiredRecycle(cutoff: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)
}
