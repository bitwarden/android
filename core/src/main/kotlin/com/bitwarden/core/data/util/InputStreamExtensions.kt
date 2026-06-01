package com.bitwarden.core.data.util

import android.os.Build
import com.bitwarden.annotation.OmitFromCoverage
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

private const val DEFAULT_BUFFER_SIZE = 8192

/**
 * Reads all bytes from this input stream and writes the bytes to the
 * given output stream in the order that they are read. On return, this
 * input stream will be at end of stream. This method does not close either
 * stream.
 *
 * This method may block indefinitely reading from the input stream, or
 * writing to the output stream. The behavior for the case where the input
 * and/or output stream is <i>asynchronously closed</i>, or the thread
 * interrupted during the transfer, is highly input and output stream
 * specific, and therefore not specified.
 *
 * If an I/O error occurs reading from the input stream or writing to the
 * output stream, then it may do so after some bytes have been read or
 * written. Consequently the input stream may not be at end of stream and
 * one, or both, streams may be in an inconsistent state. It is strongly
 * recommended that both streams be promptly closed if an I/O error occurs.
 *
 * @param outputStream the output stream, non-null
 * @return the number of bytes transferred
 * @throws IOException if an I/O error occurs when reading or writing
 * @throws NullPointerException if {@code out} is {@code null}
 */
@OmitFromCoverage
fun InputStream.sdkAgnosticTransferTo(outputStream: OutputStream): Long {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        transferTo(outputStream)
    } else {
        var transferred: Long = 0
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var read: Int
        while (this.read(buffer, 0, DEFAULT_BUFFER_SIZE).also { read = it } >= 0) {
            outputStream.write(buffer, 0, read)
            transferred += read.toLong()
        }
        transferred
    }
}
