package space.siy.hummingscore.humming

import io.reactivex.Observable

interface HummingSource {
    val notes: MutableList<Int>

    val previewSamples: MutableList<Byte>

    /** ピッチの情報を流す */
    var notesObservable: Observable<Int>

    /**  プレビュー用にダウンサンプリングした波形を流す */
    var previewSampleObservable: Observable<Byte>

    var hummingData: HummingData

    fun start()

    fun stop()

    fun rename(name: String)
}
