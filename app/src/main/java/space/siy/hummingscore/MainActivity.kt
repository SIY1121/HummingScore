package space.siy.hummingscore

import android.media.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    val player = MediaPlayer()
    var recording = false

    val hummingOption = HummingOption(120, 16, 1)

    val playerTimer = Observable.interval(20, TimeUnit.MILLISECONDS, Schedulers.computation())
    lateinit var notesStream: Observable<Int>

    var midiDevice: MidiDevice? = null
    var midiPlayer: MidiPlayer? = null

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
        }
        scoreView.hummingOption = hummingOption

        notesStream = playerTimer
            .filter { player.isPlaying }
            .observeOn(AndroidSchedulers.mainThread()).map {
                val index =
                    (player.currentPosition / 1000f * (hummingOption.bpm / 60f) * (hummingOption.noteResolution / 4)).toInt()
                recorder.tones[index]
            }

        val midiDeviceInfo = MidiDevice.getDeviceList(this)
        if (midiDeviceInfo.isNotEmpty()) {
            midiDevice = MidiDevice(this, MidiDevice.getDeviceList(this)[0])
            midiPlayer = MidiPlayer(midiDevice!!, notesStream)
        }

        val a = Observable.fromArray(1, 2, 3, 4, 5, 6, 7, 8, 9).buffer(3).map {
            println("map$it")
            it
        }.doOnNext { println("doOnNext$it") }.publish().refCount()
        a.subscribe { println("subA$it") }
        a.subscribe { println("subB$it") }
        println("hi")
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
        recorder.start()
        scoreView.notesObservable = recorder.notesObservable
        recorder.notesObservable.observeOn(AndroidSchedulers.mainThread()).subscribe {
            textView.text = it.toNoteName()
        }
        scoreView.previewSamplesObservable = recorder.previewSampleObservable
        scoreView.playerPositionObservable = playerTimer.filter { player.isPlaying }.map { player.currentPosition }
        recording = true
        button_record.setImageResource(R.drawable.ic_stop)
    }
}
