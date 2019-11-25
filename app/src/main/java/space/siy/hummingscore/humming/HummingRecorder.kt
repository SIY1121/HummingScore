package space.siy.hummingscore.humming

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import space.siy.hummingscore.wav.AudioRecorder
import space.siy.hummingscore.wav.WavWriter
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 流れてくるサンプルデータをピッチ情報に変換して流す
 * また、並行してwavファイルへの保存も行う
 * @param wavFile .wavの保存先
 * @param option 解析に係る設定
 */
class HummingRecorder(val wavFile: File, val jsonFile: File, val option: HummingOption) : HummingSource {
    val sampleRate = 44100

    private val oneFrameSampleCount = (sampleRate * 60 / option.bpm) / (option.noteResolution / 4)
    private val recorder =
        AudioRecorder(sampleRate, oneFrameSampleCount / option.previewWaveSampleRate)
    private val wavWriter = WavWriter(sampleRate, wavFile)

    override val notes = MutableList<Int>(0) { _ -> 0 }
    override val previewSamples = MutableList<Byte>(0) { _ -> 0 }

    /**  生のサンプルが流れてくる */
    lateinit var sampleObservable: Observable<ShortArray>

    /** ピッチの情報を流す */
    override lateinit var notesObservable: Observable<Int>

    /**  プレビュー用にダウンサンプリングした波形を流す */
    override lateinit var previewSampleObservable: Observable<Byte>

    private val avgParam = 3

    override fun start() {
        sampleObservable = recorder.start().observeOn(Schedulers.computation())
        sampleObservable.subscribe { samples ->
            wavWriter.writeSample(samples)
            val s = ((samples.map { it.toFloat() }.max() ?: 0f) / Short.MAX_VALUE * 256).toByte()
            previewSamples.add(s)
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
                if (tone == 0)
                    tone = notes.lastOrNull() ?: 0

                synchronized(notes) {
                    notes.add(tone)
                }
                tone
            }.publish().refCount()
        previewSampleObservable = sampleObservable.map { samples ->
            val s = ((samples.map { it.toFloat() }.max() ?: 0f) / Short.MAX_VALUE * 256).toByte()
            s
        }
    }

    override var hummingData = HummingData(jsonFile.nameWithoutExtension, option, notes, previewSamples)

    override fun stop() {
        recorder.stop()
        wavWriter.flush()

        hummingData = HummingData("新しいHumming", option, notes, previewSamples)
        hummingData.save(jsonFile)
    }
    override fun rename(name: String) {
        hummingData.name = name
        hummingData.save(jsonFile)
    }

}
