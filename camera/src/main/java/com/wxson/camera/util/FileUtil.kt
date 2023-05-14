package com.wxson.camera.util

import android.annotation.SuppressLint
import android.os.Environment
import com.wxson.camera.MyApplication
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author wxson
 * @date 2022/8/19
 * @apiNote
 */
object FileUtil {
    private val rootFolderPath = MyApplication.context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    @SuppressLint("SimpleDateFormat")
    fun createImageFile(isCrop: Boolean = false): File? {
        return try {
            val rootFile = File("$rootFolderPath${File.separator}capture")
            if (!rootFile.exists())
                rootFile.mkdirs()

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val fileName = if (isCrop) "IMG_${timeStamp}_CROP.jpg" else "IMG_$timeStamp.jpg"
            File(rootFile.absolutePath + File.separator + fileName).apply {
                if (!exists())
                    createNewFile()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @SuppressLint("SimpleDateFormat")
    fun createCameraFile(folderName: String = "camera1"): File? {
        return try {
            val rootFile = File("$rootFolderPath${File.separator}$folderName")
            if (!rootFile.exists())
                rootFile.mkdirs()

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val fileName = "IMG_$timeStamp.jpg"
            File(rootFile.absolutePath + File.separator + fileName).apply {
                if (!exists())
                    createNewFile()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @SuppressLint("SimpleDateFormat")
    fun createVideoFile(): File? {
        return try {
            val rootFile = File("$rootFolderPath${File.separator}video")
            if (!rootFile.exists())
                rootFile.mkdirs()

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val fileName = "VIDEO_$timeStamp.mp4"
            File(rootFile.absolutePath + File.separator + fileName).apply {
                if (!exists())
                    createNewFile()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

}