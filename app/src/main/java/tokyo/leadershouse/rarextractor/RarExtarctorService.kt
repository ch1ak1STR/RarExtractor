package tokyo.leadershouse.rarextractor
import android.app.Service
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import net.sf.sevenzipjbinding.ArchiveFormat
import net.sf.sevenzipjbinding.SevenZip
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
class RarExtractorService : Service() {

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == Intent.ACTION_SEND_MULTIPLE || intent?.action == Intent.ACTION_SEND) {
            val clipData = intent.clipData
            if (clipData != null) {
                for (i in 0 until clipData.itemCount) {
                    val uri = clipData.getItemAt(i).uri
                    if (uri != null) {
                        val file = getFileFromUri(uri)
                        if (file != null && file.exists() && file.name.endsWith(".rar")) {
                            // ファイルの解凍と処理を行うロジックをここに追加
                            try {
                                val destDir = getDestinationDirectory(file)
                                val format = ArchiveFormat.RAR5
                                Log.d("debug", "Archive format: $format")
                                val randomAccessFile = RandomAccessFile(file, "r")
                                val inStream = RandomAccessFileInStream(randomAccessFile)
                                val inArchive = SevenZip.openInArchive(null, inStream)
                                val extractCallback = ArchiveExtractCallback(inArchive, destDir)
                                inArchive.extract(null, false, extractCallback)
                                inArchive.close()
                                inStream.close()
                                val extractedFilePath = File(destDir, file.nameWithoutExtension)
                                Log.d(
                                    "debug",
                                    "Extracted file path: ${extractedFilePath.absolutePath}"
                                )
                                registerFileToMediaStore(extractedFilePath)
                                Toast.makeText(
                                    this,
                                    "解凍に成功しました。$extractedFilePath",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Log.d("debug", "Error")
                                Toast.makeText(this, "解凍に失敗しました。", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
        stopSelf() // サービスを停止する
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun getFileFromUri(uri: Uri): File? {
        val contentResolver = contentResolver
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    val displayName = it.getString(displayNameIndex)
                    val inputStream = contentResolver.openInputStream(uri)
                    val outputFile = File(cacheDir, displayName) // キャッシュディレクトリに一時的に保存
                    val outputStream = FileOutputStream(outputFile)
                    inputStream?.use { input -> outputStream.use { output -> input.copyTo(output) } }
                    return outputFile
                }
            }
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun registerFileToMediaStore(file: File) {
        val contentResolver: ContentResolver = contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
    }

    private fun getDestinationDirectory(file: File): File {
        val documentsDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val rareExtractorDir = File(documentsDir, "rarextractor/${file.nameWithoutExtension}")
        if (!rareExtractorDir.exists()) {
            rareExtractorDir.mkdirs()
        }
        return rareExtractorDir
    }
}
