package space.siy.hummingscore

import android.Manifest
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_main.*
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import space.siy.hummingscore.humming.HummingOption
import space.siy.hummingscore.humming.HummingPlayer
import space.siy.hummingscore.humming.HummingRecorder
import space.siy.hummingscore.humming.toNoteName
import space.siy.hummingscore.midi.MidiDevice
import space.siy.hummingscore.midi.MidiPlayer
import java.io.File

@RuntimePermissions
class MainActivity : AppCompatActivity() {
    val hummingOption = HummingOption(120, 16, 1)

    lateinit var recorder: HummingRecorder
    val player = HummingPlayer(hummingOption)

    var midiDevice: MidiDevice? = null
    var midiPlayer: MidiPlayer? = null

    var recording = false
    var hasData = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /** 録音ボタン */
        button_record.setOnClickListener {
            if (!recording)
                recordWithPermissionCheck()
            else
                stopRecord()
        }

        /** 再生ボタン */
        button_play.setOnClickListener {
            when {
                // 初期状態
                !player.isPlaying && !player.isPrepared -> {
                    player.prepare(
                        getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.absolutePath + "/humming/hoge.wav",
                        recorder.tones
                    )
                    player.onComplete = {
                        button_play.setImageResource(R.drawable.ic_play_arrow)
                    }
                    player.play()
                    button_play.setImageResource(R.drawable.ic_pause)
                }
                // 再生中
                player.isPlaying -> {
                    player.pause()
                    button_play.setImageResource(R.drawable.ic_play_arrow)
                }
                // 一時停止中
                player.isPrepared -> {
                    player.play()
                    button_play.setImageResource(R.drawable.ic_pause)
                }
            }
        }

        /** midi接続ボタン */
        button_midi.setOnClickListener {
            if (midiDevice != null) return@setOnClickListener

            val midiDeviceInfo = MidiDevice.getDeviceList(this)

            AlertDialog.Builder(this)
                .setTitle("Midiデバイスを選択してください")
                .setItems(midiDeviceInfo.map { "Midi Port ${it.id}" }.toTypedArray()) { _, which ->
                    midiDevice = MidiDevice(
                        this,
                        MidiDevice.getDeviceList(this)[which]
                    )
                    midiPlayer = MidiPlayer(midiDevice!!, player.notesStream)
                    button_midi.compoundDrawablesRelative[0].colorFilter =
                        PorterDuffColorFilter(resources.getColor(R.color.colorAccent), PorterDuff.Mode.SRC_IN)

                    Toast.makeText(this, "Midi Port に接続しました", Toast.LENGTH_LONG).show()
                }.show()
        }

        button_play.isEnabled = false

        scoreView.hummingOption = hummingOption
        scoreView.playerPositionObservable = player.playerTimer.observeOn(AndroidSchedulers.mainThread())
        player.notesStream.observeOn(AndroidSchedulers.mainThread()).subscribe { textView.text = it.toNoteName() }
    }

    @NeedsPermission(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    fun record() {
        File(getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.absolutePath + "/humming").mkdir()
        val file = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.absolutePath + "/humming/hoge.wav")
        recorder = HummingRecorder(file, hummingOption)
        recorder.start()
        scoreView.notesObservable = recorder.notesObservable
        recorder.notesObservable.observeOn(AndroidSchedulers.mainThread()).subscribe {
            textView.text = it.toNoteName()
        }
        scoreView.previewSamplesObservable = recorder.previewSampleObservable
        recording = true
        button_record.setImageResource(R.drawable.ic_stop)
    }

    private fun stopRecord() {
        recorder.stop()
        recording = false
        hasData = true

        button_record.setImageResource(R.drawable.ic_fiber_manual_record)
        button_record.isEnabled = false
        button_record.alpha = 0.5f
        button_play.alpha = 1f
        button_play.isEnabled = true

        Toast.makeText(this, "開発中につき、録音し直すには再起動してください", Toast.LENGTH_LONG).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }
}
