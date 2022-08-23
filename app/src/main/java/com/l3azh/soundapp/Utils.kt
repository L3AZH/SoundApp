package com.l3azh.soundapp

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ShortBuffer

class Utils {
    companion object {
        fun exportToFile(
            context: Context,
            data: ByteArray,
            extensionName: String,
            fileName: String
        ) {
            val file = File(context.filesDir, "$fileName.$extensionName")
            val fileOutputStream = FileOutputStream(file)
            try {
                fileOutputStream.write(data, 0, data.size)
                fileOutputStream.flush()
            } catch (e: Exception) {
                e.stackTrace
            } finally {
                fileOutputStream.close()
            }
        }

        fun createFile(context: Context, extensionName: String, fileName: String): File {
            return File(context.filesDir, "$fileName.$extensionName")
        }

        fun writeToFile(context: Context, file: File, byteArray: ByteArray) {
            context.openFileOutput(file.name, Context.MODE_PRIVATE).use {
                it.write(byteArray, 0, byteArray.size)
            }
        }

        fun getTotalDataOfListShortArray(listShortArray: List<ShortArray>):Long{
            var total:Long = 0
            for (shortArray in listShortArray){
                total += shortArray.size
            }
            return total
        }

        fun covertShortArrayToByteArray(shortArray: ShortArray): ByteArray {
            val byteBuffer = ByteBuffer.allocate(shortArray.size * 2)
            for (short in shortArray) {
                byteBuffer.putShort(short)
            }
            return byteBuffer.array()
        }

        fun convertListShortArrayToShortBuffer(listShortArray: List<ShortArray>): ShortBuffer {
            var totalSize = 0
            for (shortArray in listShortArray) {
                totalSize += shortArray.size
            }
            val shortBuffer: ShortBuffer = ShortBuffer.allocate(totalSize)
            for (shortArray in listShortArray) {
                for(shortData in shortArray){
                    shortBuffer.put(shortData)
                }
            }
            return shortBuffer
        }

        fun convertListShortArrayToByteArray(listShortArray: List<ShortArray>): ByteArray {
            var totalSizeBuffer = 0
            for(array in listShortArray){
                totalSizeBuffer += array.size
            }
            val byteBuffer = ByteBuffer.allocate(totalSizeBuffer*2)
            for (arr in listShortArray){
                for(shortData in arr){
                    byteBuffer.putShort(shortData)
                }
            }
            return byteBuffer.array()
        }
    }
}