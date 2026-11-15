package com.dreamtrail.app.data

import com.dreamtrail.core.engine.ActionOutcome
import com.dreamtrail.core.save.SaveBackupCodec
import java.io.InputStream
import java.io.OutputStream
import java.io.ByteArrayOutputStream

/** Run on Dispatchers.IO. Enforce the byte limit before allocating/decoding the entire document. */
object BackupFiles {
    fun read(open: () -> InputStream?): ActionOutcome<String> = try {
        val text = (open() ?: error("无法打开存档文件")).use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var total = 0
            while (true) {
                val read = input.read(buffer, 0, minOf(buffer.size, SaveBackupCodec.MAX_IMPORT_BYTES + 1 - total))
                if (read < 0) break
                total += read
                check(total <= SaveBackupCodec.MAX_IMPORT_BYTES) { "存档文件体积超出上限，未修改当前进度" }
                output.write(buffer, 0, read)
            }
            output.toString(Charsets.UTF_8.name())
        }
        ActionOutcome.Success(text)
    } catch (e: Exception) { ActionOutcome.Failure(e.message ?: "读取存档文件失败") }

    fun write(open: () -> OutputStream?, text: String): ActionOutcome<Unit> = try {
        (open() ?: error("无法创建存档文件")).use { it.write(text.toByteArray(Charsets.UTF_8)) }
        ActionOutcome.Success(Unit)
    } catch (e: Exception) { ActionOutcome.Failure(e.message ?: "保存文件失败，请重新选择位置") }
}
