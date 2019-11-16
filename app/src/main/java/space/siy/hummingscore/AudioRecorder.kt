package space.siy.hummingscore

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.abs

class AudioRecorder(val sampleRate: Int, val oneFrameDataCount: Int) {
    val oneFrameSizeInByte = oneFrameDataCount * 2
    private val bufferSizeInByte =
        AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
    val audioArray = ShortArray(oneFrameDataCount)

    private val audioRecord = AudioRecord(
        MediaRecorder.AudioSource.MIC,
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
        bufferSizeInByte * 2
    )

    fun start() = Observable.create<ShortArray> {
        audioRecord.positionNotificationPeriod = oneFrameDataCount
        audioRecord.notificationMarkerPosition = 4000
        audioRecord.setRecordPositionUpdateListener(object : AudioRecord.OnRecordPositionUpdateListener {
            override fun onPeriodicNotification(recorder: AudioRecord) {
                recorder.read(audioArray, 0, oneFrameDataCount)
                it.onNext(audioArray)
            }

            override fun onMarkerReached(recorder: AudioRecord) {}
        })
        audioRecord.startRecording()
        audioRecord.read(audioArray, 0, oneFrameDataCount)
    }

    fun stop() {
        audioRecord.stop()
    }
}