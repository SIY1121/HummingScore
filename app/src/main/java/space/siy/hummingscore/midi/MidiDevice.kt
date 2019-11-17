package space.siy.hummingscore.midi

import android.content.Context
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiInputPort
import android.media.midi.MidiManager
import android.os.Handler

/**
 * Midiデバイス
 */
class MidiDevice(private val context: Context, val info: MidiDeviceInfo) {
    companion object {
        fun getDeviceList(context: Context): Array<MidiDeviceInfo>
                = (context.getSystemService(Context.MIDI_SERVICE) as MidiManager).devices
    }

    private val manager = context.getSystemService(Context.MIDI_SERVICE) as MidiManager
    private lateinit var midiDevice: MidiDevice
    private lateinit var inputPort: MidiInputPort

    init {
        manager.openDevice(info, {
            midiDevice = it
            inputPort = it.openInputPort(0)
        }, Handler())
    }

    /**
     * キーを押した信号を送る
     */
    fun noteOn(tone: Int) {
        val channel = 1
        val velocity = 127.toByte()
        val buf = byteArrayOf((0x90 + channel - 1).toByte(), (tone + 21).toByte(), velocity)
        inputPort.send(buf, 0, buf.size)
    }

    /**
     * キーを離した信号を送る
     */
    fun noteOff(tone: Int) {
        val channel = 1
        val velocity = 127.toByte()
        val buf = byteArrayOf((0x80 + channel - 1).toByte(), (tone + 21).toByte(), velocity)
        inputPort.send(buf, 0, buf.size)
    }
}
