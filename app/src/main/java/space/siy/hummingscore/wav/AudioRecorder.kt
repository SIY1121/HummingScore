package space.siy.hummingscore.wav

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.HandlerThread
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.abs

/**
 * マイクから音声を拾い、サンプルをStreamに流す責務を持つ
 * @param sampleRate サンプルレート
 * @param oneFrameDataCount 1フレームあたりのサンプル数
 */
class AudioRecorder(val sampleRate: Int, val oneFrameDataCount: Int) {

    private val oneFrameSizeInByte = oneFrameDataCount * 2
    private val bufferSizeInByte =
        AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
    private val audioArray = ShortArray(oneFrameDataCount)
    private val thread = HandlerThread("AudioRecordThread")

    private val audioRecord = AudioRecord(
        MediaRecorder.AudioSource.MIC,
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
        bufferSizeInByte * 2
    )


    init {
        thread.start()
    }


    fun start() = Observable.create<ShortArray> {
        audioRecord.positionNotificationPeriod = oneFrameDataCount
        audioRecord.setRecordPositionUpdateListener(object : AudioRecord.OnRecordPositionUpdateListener {
            override fun onPeriodicNotification(recorder: AudioRecord) {
                recorder.read(audioArray, 0, oneFrameDataCount)
                it.onNext(audioArray)
            }
            override fun onMarkerReached(recorder: AudioRecord) {}
        }, Handler(thread.looper))
        audioRecord.startRecording()
        audioRecord.read(audioArray, 0, oneFrameDataCount)
    }.publish().refCount()


    fun stop() {
        audioRecord.stop()
    }
}
