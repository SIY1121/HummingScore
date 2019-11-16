package space.siy.hummingscore

import android.media.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    val player = MediaPlayer()
    var recording = false
    val playerTimer = Timer()
    var playerTimerTask: TimerTask? = null

    val hummingOption = HummingOption(120, 16)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        button_record.setOnClickListener {
            record()
        }
        button_play.setOnClickListener {
            player.setDataSource(getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.absolutePath + "/humming/hoge.wav")
            player.setOnCompletionListener {
                player.stop()
                player.reset()
            }
            player.prepare()
            player.start()

            playerTimerTask = playerTimer.scheduleAtFixedRate(0L, 20L) {
                scoreView.playerPosition = player.currentPosition
            }
        }
        scoreView.hummingOption = hummingOption
    }

    lateinit var recorder: HummingRecorder
    private fun record() {
        if (recording) {
            recorder.stop()
            recording = false
            button_record.setImageResource(R.drawable.ic_fiber_manual_record)
            return
        }
        File(getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.absolutePath + "/humming").mkdir()
        val file = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.absolutePath + "/humming/hoge.wav")
        recorder = HummingRecorder(file, hummingOption)
        recorder.start().observeOn(AndroidSchedulers.mainThread()).subscribe {
            scoreView.addAndDraw(it)
            textView.text = it.note.toNoteName()
        }
        recording = true
        button_record.setImageResource(R.drawable.ic_stop)
    }
}
