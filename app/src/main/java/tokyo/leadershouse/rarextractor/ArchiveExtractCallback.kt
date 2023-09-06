package tokyo.leadershouse.rarextractor
import android.util.Log
import net.sf.sevenzipjbinding.ExtractAskMode
import net.sf.sevenzipjbinding.ExtractOperationResult
import net.sf.sevenzipjbinding.IArchiveExtractCallback
import net.sf.sevenzipjbinding.IInArchive
import net.sf.sevenzipjbinding.ISequentialOutStream
import net.sf.sevenzipjbinding.PropID
import net.sf.sevenzipjbinding.SevenZipException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
class ArchiveExtractCallback(
    private val inArchive: IInArchive,
    private val destDir: File,
) : IArchiveExtractCallback {
    private var uos: OutputStream? = null
    override fun getStream(index: Int, extractAskMode: ExtractAskMode): ISequentialOutStream {
        val path     = inArchive.getStringProperty(index, PropID.PATH)
        val isDir    = inArchive.getProperty(index, PropID.IS_FOLDER) as Boolean
        val destPath = File(destDir, path)
        Log.d("debug", "Index: $index")
        Log.d("debug", "Extract Ask Mode: $extractAskMode")
        Log.d("debug", "File Path: $path")
        Log.d("debug", "Is Directory: $isDir")
        Log.d("debug", "Destination Path: ${destPath.absolutePath}")
        try {
            if (isDir) { destPath.mkdirs() }
            else {
                destPath.parentFile?.mkdirs()
                destPath.createNewFile()
                uos = FileOutputStream(destPath)
            }
            return ISequentialOutStream { bytes ->
                try {
                    uos?.write(bytes)
                    bytes.size
                } catch (e: IOException) { throw SevenZipException(e) }
            }
        } catch (e: IOException) { throw SevenZipException(e) }
    }
    override fun setOperationResult(extractOperationResult: ExtractOperationResult) {
        Log.d("debug", "Operation Result: $extractOperationResult")
        if (extractOperationResult != ExtractOperationResult.OK) {
            throw SevenZipException(extractOperationResult.toString())
        }
    }
    override fun setTotal(total: Long) {}
    override fun setCompleted(complete: Long) {}
    override fun prepareOperation(extractAskMode: ExtractAskMode) {}
}
