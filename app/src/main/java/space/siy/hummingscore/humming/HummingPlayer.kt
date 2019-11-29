package space.siy.hummingscore.humming

import android.media.MediaPlayer
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import kotlin.math.min

/**  指定された音源とピッチ情報を再生する */
class HummingPlayer(var hummingOption: HummingOption) {

    private val player = MediaPlayer()
    private var notes = List<Int>(0) { _ -> 0 }

    /**
     * 再生中にプレイヤーの現在位置を流すストリーム
     */
    val playerTimer = Observable.interval(20, TimeUnit.MILLISECONDS, Schedulers.computation())
        .filter { player.isPlaying || tmp }
        .map { player.currentPosition }

    /**
     * 再生位置の音程を流すストリーム
     */
    val notesStream = playerTimer.filter { player.isPlaying }.map {
        val index =
            (player.currentPosition / 1000f * (hummingOption.bpm / 60f) * (hummingOption.noteResolution / 4)).toInt()
        notes[min(index, notes.size - 1)]
    }

    val isPlaying
        get() = player.isPlaying
    var isPrepared = false

    var onComplete: (() -> Unit)? = null

    var tmp = false

    init {
        playerTimer.subscribe { tmp = false }
    }

    fun prepare(path: String, _notes: List<Int>) {
        notes = _notes
        player.setDataSource(path)
        player.prepare()
        player.setOnCompletionListener {
            player.stop()
            player.reset()
            isPrepared = false
            onComplete?.invoke()
        }
        isPrepared = true
    }

    fun play() = player.start()
    fun pause() = player.pause()

    fun seek(p: Int) {
        player.seekTo(p)
        tmp = true
    }
}
