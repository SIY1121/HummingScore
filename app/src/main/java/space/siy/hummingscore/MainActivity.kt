package space.siy.hummingscore

import android.media.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    lateinit var audioRecord: AudioRecord
    val sampleRate = 44100
    val frameRate = 30
    val oneFrameDataCount = sampleRate / frameRate
    val oneFrameSizeInByte = oneFrameDataCount * 2
    val bufferSizeInByte =
        AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
    val audioArray = ShortArray(oneFrameDataCount)
    lateinit var writer: WavWriter
    var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        button.setOnClickListener {
            record()
        }
    }

    private fun record() {
        if (isRecording) {
            audioRecord.stop()
            writer.flush()
            return
        }
        File(getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.absolutePath + "/humming").mkdir()
        writer = WavWriter(
            sampleRate,
            File(getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.absolutePath + "/humming/hoge.wav")
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSizeInByte
        )
        audioRecord.positionNotificationPeriod = oneFrameDataCount
        audioRecord.notificationMarkerPosition = 4000
        audioRecord.setRecordPositionUpdateListener(object : AudioRecord.OnRecordPositionUpdateListener {
            override fun onPeriodicNotification(recorder: AudioRecord) {
                recorder.read(audioArray, 0, oneFrameDataCount)
                val a = audioArray.map { abs(it.toInt()) }.max() ?: 0
                progress_bar.progress = (a.toFloat() / Short.MAX_VALUE * 100).toInt()
                writer.writeSample(audioArray)
            }

            override fun onMarkerReached(recorder: AudioRecord) {}
        })
        audioRecord.startRecording()
        audioRecord.read(audioArray, 0, oneFrameDataCount)
        isRecording = true
    }
}
