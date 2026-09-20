package com.emohappy.pulse.data

import android.content.Context
import android.os.Environment
import com.emohappy.pulse.model.PulseBackupDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

class AutoBackupManager(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val internalBackupFile: File
        get() = File(context.filesDir, "pulse_auto_backup.json")

    private val publicDownloadBackupFile: File
        get() = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "pulse_backup.json"
        )

    private val candidateFiles: List<File>
        get() = listOfNotNull(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let { File(it, "pulse_backup.json") },
            context.getExternalFilesDir(null)?.let { File(it, "pulse_backup.json") },
            File("/sdcard/Download/pulse_backup.json"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "pulse_backup.json"),
            internalBackupFile
        )

    /**
     * 后台静默将当前最新数据镜像写入本地内部存储与手机公用/专属外部存储
     */
    suspend fun saveAutoBackup(jsonContent: String) = withContext(Dispatchers.IO) {
        runCatching {
            // 1. 始终写入 App 私有目录（安全、快速、无需任何权限）
            internalBackupFile.writeText(jsonContent, Charsets.UTF_8)

            // 2. 写入 App 专属外部存储（卸载可选择保留，无需运行时权限）
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let { dir ->
                File(dir, "pulse_backup.json").writeText(jsonContent, Charsets.UTF_8)
            }

            // 3. 尝试镜像写入外部公共下载目录（便于用户直接查阅或跨机导出）
            if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                publicDownloadBackupFile.writeText(jsonContent, Charsets.UTF_8)
            }
        }
    }

    /**
     * 检查是否存在可恢复的自动备份文件
     */
    suspend fun getAvailableBackup(): String? = withContext(Dispatchers.IO) {
        val candidate = candidateFiles.firstOrNull { file ->
            val exists = runCatching { file.exists() && file.length() > 0 }.getOrDefault(false)
            android.util.Log.d("PulseBackup", "Checking file: ${file.absolutePath}, exists: $exists")
            exists
        }

        candidate?.let {
            val content = runCatching { it.readText(Charsets.UTF_8) }.getOrNull()
            android.util.Log.d("PulseBackup", "Read candidate: ${it.absolutePath}, bytes: ${content?.length}")
            content
        }
    }

    /**
     * 读取备份中的消费条数
     */
    suspend fun getBackupTransactionCount(): Int = withContext(Dispatchers.IO) {
        val content = getAvailableBackup() ?: return@withContext 0
        runCatching {
            val dto = json.decodeFromString<PulseBackupDto>(content)
            android.util.Log.d("PulseBackup", "Decoded dto: ${dto.transactions.size} transactions")
            dto.transactions.size
        }.onFailure {
            android.util.Log.e("PulseBackup", "Failed to decode backup dto", it)
        }.getOrDefault(0)
    }
}
