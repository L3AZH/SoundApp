package com.l3azh.soundapp

import android.media.AudioRecord
import android.media.AudioTrack
import kotlinx.coroutines.Job
import java.io.File

data class RecordResult(
    var record:AudioRecord? = null,
    var audioBuffer:MutableList<ShortArray>? = null,
    var fileRecord:File? = null,
    var jobRecord:Job?,
    var audioTrack: AudioTrack?
)