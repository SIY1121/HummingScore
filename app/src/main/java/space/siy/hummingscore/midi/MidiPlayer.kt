package space.siy.hummingscore.midi

import io.reactivex.Observable

/**
 * 流れてくるnotesを実際にMidiデバイスに流す
 * @param device 流すデバイス
 * @param stream notesが流れてくるストリーム
 */
class MidiPlayer(val device: MidiDevice, val stream: Observable<Int>) {
    private var prev = -1

    init {
        // TODO サブスクリプションの管理
        stream.subscribe {
            if (prev != it) {
                device.noteOff(prev)
                if (it > 0)
                    device.noteOn(it)
            }
            prev = it
        }
    }
}
