package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- entities ---

@Entity(tableName = "models")
data class ModelEntity(
    @PrimaryKey val id: String, // unique string, e.g. "Qwen/Qwen2.5-1.5B-Instruct-GGUF"
    val name: String,
    val author: String,
    val sizeGb: Double,
    val quantization: String, // "4-bit (Q4_K_M)", "1-bit (Q1_K)", "MNN FP16", "FP16" etc.
    val format: String, // "GGUF", "MNN", "ONNX", "GGML"
    val isDownloaded: Boolean = false,
    val filePath: String = "",
    val isCustom: Boolean = false,
    val downloadProgress: Int = 0,
    val minRamRequiredGb: Double = 1.0,
    val pagedRomRequiredGb: Double = 3.0,
    val downloads: Int = 0,
    val likes: Int = 0
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String, // "user", "assistant" or model id
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val tokensPerSec: Double = 0.0,
    val ramUsedMb: Int = 0,
    val romPagedGb: Double = 0.0,
    val vramUsedMb: Int = 0,
    val activeThreads: Int = 4,
    val executionMode: String = "ROM Swap" // "RAM only", "Memory Mapped (Mmap)", "ROM Swap (Virtual Paging)"
)

@Entity(tableName = "engine_logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tag: String, // "ENGINE", "MMAP", "SWAP", "GPU", "OOM_PREVENT"
    val level: String, // "INFO", "WARN", "DEBUG", "ERROR"
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

// --- DAO ---

@Dao
interface RomAiDao {
    // Models
    @Query("SELECT * FROM models ORDER BY isDownloaded DESC, name ASC")
    fun getAllModelsFlow(): Flow<List<ModelEntity>>

    @Query("SELECT * FROM models WHERE id = :id LIMIT 1")
    suspend fun getModelById(id: String): ModelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(model: ModelEntity)

    @Query("DELETE FROM models WHERE id = :id")
    suspend fun deleteModelById(id: String)

    @Query("UPDATE models SET downloadProgress = :progress, isDownloaded = :isDownloaded, filePath = :path WHERE id = :id")
    suspend fun updateDownloadStatus(id: String, progress: Int, isDownloaded: Boolean, path: String)

    // Chat
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getChatMessagesFlow(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearHistory()

    // Logs
    @Query("SELECT * FROM engine_logs ORDER BY timestamp ASC")
    fun getLogsFlow(): Flow<List<LogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LogEntity)

    @Query("DELETE FROM engine_logs")
    suspend fun clearLogs()
}

// --- Database Configuration ---

@Database(entities = [ModelEntity::class, ChatMessage::class, LogEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): RomAiDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rom_ai_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
