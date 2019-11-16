package space.siy.hummingscore

import kotlin.experimental.and
import java.io.*


class WavWriter(private val sampleRate: Int, private val file: File) {
    private val buf = ByteArrayOutputStream()
    fun writeSample(arr: ShortArray) {
        synchronized(buf){
            buf.write(arr.toByteArray())
        }
    }

    fun flush() {
        val outFile = DataOutputStream(BufferedOutputStream(FileOutputStream(file)))

        val mySubChunk1Size = 16
        val myBitsPerSample = 16
        val myFormat = 1
        val myChannels = 1
        val mySampleRate = sampleRate
        val myByteRate = mySampleRate * myChannels * myBitsPerSample / 8
        val myBlockAlign = (myChannels * myBitsPerSample / 8)

        val clipData = buf.toByteArray()

        val myDataSize = clipData.size
        val myChunk2Size = myDataSize * myChannels * myBitsPerSample / 8
        val myChunkSize = 36 + myChunk2Size

        outFile.writeBytes("RIFF")
        outFile.write(myChunkSize.toByteArray(), 0, 4)
        outFile.writeBytes("WAVE")                                 // 08 - WAVE
        outFile.writeBytes("fmt ")                                 // 12 - fmt
        outFile.write(mySubChunk1Size.toByteArray(), 0, 4)  // 16 - size of this chunk
        outFile.write(
            shortArrayOf(myFormat.toShort()).toByteArray(),
            0,
            2
        )     // 20 - what is the audio format? 1 for PCM = Pulse Code Modulation
        outFile.write(
            shortArrayOf(myChannels.toShort()).toByteArray(),
            0,
            2
        )   // 22 - mono or stereo? 1 or 2?  (or 5 or ???)
        outFile.write(mySampleRate.toByteArray(), 0, 4)     // 24 - samples per second (numbers per second)
        outFile.write(myByteRate.toByteArray(), 0, 4)       // 28 - bytes per second
        outFile.write(myBlockAlign.toByteArray(), 0, 2) // 32 - # of bytes in one sample, for all channels
        outFile.write(
            shortArrayOf(myBitsPerSample.toShort()).toByteArray(),
            0,
            2
        )  // 34 - how many bits in a sample(number)?  usually 16 or 24
        outFile.writeBytes("data")                                 // 36 - data
        outFile.write(myDataSize.toByteArray(), 0, 4)       // 40 - how big is this data chunk
        outFile.write(clipData)
        outFile.flush()
        outFile.close()
    }

    private fun ShortArray.toByteArray(): ByteArray {
        val byteArray = ByteArray(this.size * 2)
        this.forEachIndexed { index, sh ->
            byteArray[index * 2] = (sh and 0xff).toByte()
            byteArray[index * 2 + 1] = ((sh.toInt() shr 8) and 0xff).toByte()
        }
        return byteArray
    }

    private fun Int.toByteArray(): ByteArray {
        val b = ByteArray(4)
        b[0] = (this and 0x00FF).toByte()
        b[1] = (this shr 8 and 0x000000FF).toByte()
        b[2] = (this shr 16 and 0x000000FF).toByte()
        b[3] = (this shr 24 and 0x000000FF).toByte()
        return b
    }
}
