package data

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.nio.charset.StandardCharsets

object TestDataReader {
    fun getTestData(fileName: String): TestData {
        val context = InstrumentationRegistry.getInstrumentation().context.assets
        val inputStream: InputStream = context.open(fileName)
        val size = inputStream.available()
        val buffer = ByteArray(size)
        inputStream.read(buffer)
        inputStream.close()
        val jsonString = String(buffer, StandardCharsets.UTF_8)
        return Json.decodeFromString<TestData>(jsonString)
    }
}
