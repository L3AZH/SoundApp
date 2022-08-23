package com.l3azh.soundapp

import android.Manifest
import android.content.pm.PackageManager
import android.media.*
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.*
import org.apache.commons.io.IOUtil
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import java.util.*


class RecordService(private val activity: ComponentActivity) {

    companion object {
        const val TAG = "RECORD_SERVICE"
    }

    private var _defaultRate = 44100
    var defaultRate: Int
        get() = this._defaultRate
        set(value) {
            this._defaultRate = value
        }
    private var _bufferSize: Int = -1
    private var _listRecordInstance: MutableMap<Long, RecordResult>? = null
    val listRecordInstance: MutableMap<Long, RecordResult>?
        get() {
            if (_listRecordInstance == null) {
                _listRecordInstance = mutableMapOf()
            }
            return _listRecordInstance!!
        }
    private var _onRequestPermission: ActivityResultLauncher<String>

    init {
        _bufferSize = getBufferSizeRecord()
        _listRecordInstance = mutableMapOf()
        _onRequestPermission = initPermissionRequest(activity)
    }

    private fun getBufferSizeRecord(): Int {
        var size: Int = -1
        if (size == -1) {
            size = AudioRecord.getMinBufferSize(
                _defaultRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (_bufferSize == AudioRecord.ERROR || _bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                size = _defaultRate * 2
            }
        }
        Log.d(TAG, "getBufferSizeRecord: $size")
        return size
    }

    private fun getBufferSizePlay(): Int {
        var size: Int = -1
        if (size == -1) {
            size = AudioTrack.getMinBufferSize(
                _defaultRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (_bufferSize == AudioTrack.ERROR || _bufferSize == AudioTrack.ERROR_BAD_VALUE) {
                size = _defaultRate * 2
            }
        }
        Log.d(TAG, "getBufferSizePlay: $size")
        return size
    }

    private fun initPermissionRequest(activity: ComponentActivity): ActivityResultLauncher<String> {
        return activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {}
    }

    fun createAndStartRecord(): Long {
        if (activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            _onRequestPermission.launch(Manifest.permission.RECORD_AUDIO)
            return -1
        } else {
            val id = Date().time

            val recordAudio = AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.DEFAULT)
                .setAudioFormat(
                    AudioFormat.Builder().setSampleRate(_defaultRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO).build()
                )
                .setBufferSizeInBytes(_bufferSize)
                .build()
            recordAudio.startRecording()

            Log.d(TAG, "Create Play back for record.... ")
            val audioTrack = AudioTrack.Builder()
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(_defaultRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
                )
                .setBufferSizeInBytes(getBufferSizePlay())
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
                ).build()
            Log.d(TAG, "Created task record with id: $id")
            val file = Utils.createFile(activity, "pcm", "recordfile-${Date().time}")
            _listRecordInstance!![id] =
                RecordResult(recordAudio, mutableListOf(), file, null, audioTrack)
            Log.d(TAG, "Start Recording ...")

            val jobRecord = CoroutineScope(Dispatchers.IO).launch {
                while (isActive) {
                    val audioBuffer = ShortArray(_bufferSize / 2)
                    val numberOfShort = _listRecordInstance!![id]!!.record!!.read(
                        audioBuffer,
                        0,
                        audioBuffer.size
                    )
                    _listRecordInstance!![id]!!.audioBuffer!!.add(audioBuffer)
                    Log.d(TAG, "Recording $numberOfShort...")
                }
            }
            _listRecordInstance!![id]!!.jobRecord = jobRecord
            return id
        }
    }

    fun stopRecord(id: Long) {
        Log.d(TAG, "stopRecord id: $id")
        Log.d(TAG, "the record size: ${_listRecordInstance!![id]!!.audioBuffer!!.size}")
        _listRecordInstance!![id]!!.jobRecord!!.cancel()
        _listRecordInstance!![id]!!.record!!.stop()
        Log.d(TAG, "Write to file: ${_listRecordInstance!![id]!!.fileRecord!!.name}")
        Utils.writeToFile(
            activity,
            _listRecordInstance!![id]!!.fileRecord!!,
            Utils.convertListShortArrayToByteArray(_listRecordInstance!![id]!!.audioBuffer!!)
        )
        _listRecordInstance!![id]!!.record!!.release()
    }

    fun playRecord(id: Long) {
        if (_listRecordInstance!![id]!!.jobRecord!!.isActive) {
            Log.e(TAG, "playRecord: the record still continuing please stop record before play !!")
            return
        } else {
            Log.d(TAG, "playRecord with id: $id")
            val playJob = CoroutineScope(Dispatchers.IO).launch {
                _listRecordInstance!![id]!!.audioTrack!!.play()
                var buffer = ShortArray(getBufferSizePlay())
                val sampleShortArray =
                    getAudioSampleFromFile(_listRecordInstance!![id]!!.fileRecord!!)
                val sample =
                    ShortBuffer.wrap(sampleShortArray)
                sample.rewind()
                var limit = sampleShortArray.size
                var totalWritten = 0
                while (sample.position() < limit && isActive) {
                    val numSamplesLeft: Int = limit.toInt() - sample.position()
                    var samplesToWrite: Int
                    if (numSamplesLeft >= buffer.size) {
                        sample.get(buffer)
                        samplesToWrite = buffer.size
                    } else {
                        for (i in (numSamplesLeft until buffer.size)) {
                            buffer[i] = 0
                        }
                        sample.get(buffer, 0, numSamplesLeft)
                        samplesToWrite = numSamplesLeft
                    }
                    totalWritten += samplesToWrite
                    _listRecordInstance!![id]!!.audioTrack!!.write(buffer, 0, samplesToWrite)

                }
                _listRecordInstance!![id]!!.audioTrack!!.stop()
                _listRecordInstance!![id]!!.audioTrack!!.release()
            }
            Log.d(TAG, "playRecord with id: $id")
        }
    }

    fun deleteRecordInstance(id: Long) {
        _listRecordInstance!!.remove(id)
    }

    fun clearRecordInstance() {
        _listRecordInstance!!.clear()
    }

    private fun getAudioSampleFromFile(file: File): ShortArray {
        val fileInputStream: InputStream = FileInputStream(file)
        val byteArray = IOUtil.toByteArray(fileInputStream)
        val shortBuffer = ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val shortArray = ShortArray(shortBuffer.limit())
        shortBuffer.get(shortArray)
        return shortArray
    }

}