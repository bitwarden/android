package data

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.nio.charset.StandardCharsets

object TestDataReader {
    fun getTestData(fileName: String): TestData {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        val jsonString = assets
            .open(fileName)
            .use { inputStream ->
                inputStream.bufferedReader(StandardCharsets.UTF_8)
                    .readText()
            }
        return Json.decodeFromString<TestData>(jsonString)
    }
}
