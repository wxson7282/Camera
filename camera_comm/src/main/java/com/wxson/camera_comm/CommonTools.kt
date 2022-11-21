package com.wxson.camera_comm

import okio.Buffer
import okio.ByteString
import java.io.*
import kotlin.experimental.and


/**
 * @author wxson
 * @date 2022/10/23
 * @apiNote
 */
object CommonTools {
    /**
     * 将byte[]转换为16进制字符串
     * @param bytes
     * @return
     */
    fun bytesToHex(bytes: ByteArray): String {
        val buf = StringBuilder(bytes.size * 2)
        for (b in bytes) { // 使用String的format方法进行转换
            buf.append(String.format("%02x", b and 0xff.toByte()))
        }
        return buf.toString()
    }

    /**
     * 将可序列化对象转化为ByteArray
     * 如果转化失败，返回null
     */
    fun objectToByteArray(obj: Any): ByteArray? {
        try {
            val buffer = Buffer()
            buffer.outputStream().use { outputStream ->
                ObjectOutputStream(outputStream).use {
                    it.writeObject(obj)
                    it.flush()
                }
            }
            return buffer.readByteArray()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * 将ByteArray转化为可序列化对象
     * 如果转化失败，返回null
     */
    fun byteArrayToObject(byteArray: ByteArray) : Any? {
        try {
            ByteArrayInputStream(byteArray).use { byteArrayInputStream ->
                ObjectInputStream(byteArrayInputStream).use {
                    return it.readObject()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * 将可序列化对象转化为ByteString
     * 如果转化失败，返回null
     */
    fun objectToByteString(obj: Any) : ByteString? {
        try {
            val buffer = Buffer()
            buffer.outputStream().use { outputStream ->
                ObjectOutputStream(outputStream).use {
                    it.writeObject(obj)
                    it.flush()
                }
            }
            return buffer.readByteString()
        }catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * 将ByteString转化为可序列化对象
     * 如果转化失败，返回null
     */
    fun byteStringToObject(byteString: ByteString) : Any? {
        try {
            Buffer().inputStream().use { inputStream ->
                ObjectInputStream(inputStream).use {
                    return it.readObject()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }
        return null
    }
}