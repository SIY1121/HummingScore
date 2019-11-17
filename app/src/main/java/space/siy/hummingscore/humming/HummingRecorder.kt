package space.siy.hummingscore.humming

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import space.siy.hummingscore.wav.AudioRecorder
import space.siy.hummingscore.wav.WavWriter
import java.io.File

/**
 * 流れてくるサンプルデータをピッチ情報に変換して流す
 * また、並行してwavファイルへの保存も行う
 * @param file .wavの保存先
 * @param option 解析に係る設定
 */
class HummingRecorder(val file: File, val option: HummingOption) {
    val sampleRate = 44100

    private val oneFrameSampleCount = (sampleRate * 60 / option.bpm) / (option.noteResolution / 4)
    private val recorder =
        AudioRecorder(sampleRate, oneFrameSampleCount / option.previewWaveSampleRate)
    private val wavWriter = WavWriter(sampleRate, file)

    val tones = MutableList<Int>(0) { _ -> 0 }

    /**  生のサンプルが流れてくる */
    lateinit var sampleObservable: Observable<ShortArray>

    /** ピッチの情報を流す */
    lateinit var notesObservable: Observable<Int>

    /**  プレビュー用にダウンサンプリングした波形を流す */
    lateinit var previewSampleObservable: Observable<Byte>

    private val avgParam = 3

    fun start() {
        sampleObservable = recorder.start().observeOn(Schedulers.computation())
        sampleObservable.subscribe { samples ->
            wavWriter.writeSample(samples)
        }
        notesObservable = sampleObservable
            .buffer(option.previewWaveSampleRate)
            .map { _samples ->
                val sampleCount = _samples.sumBy { it.size }
                var samples = ShortArray(0)
                val i = 0
                _samples.forEach {
                    samples += it
                }
                var tone = PitchDetector.analyze(samples)
                if(tone == 0)
                    tone = tones.lastOrNull() ?: 0

                synchronized(tones) {
                    tones.add(tone)
                }
                tone
            }.publish().refCount()
        previewSampleObservable = sampleObservable.map { samples ->
            ((samples.map { it.toFloat() }.max() ?: 0f) / Short.MAX_VALUE * 256).toByte()
        }
    }

    fun stop() {
        recorder.stop()
        wavWriter.flush()
    }
}
