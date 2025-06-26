package data

import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import java.io.InputStreamReader
import java.nio.file.Paths

object TestDataReader {
    fun getTestData(fileName: String): TestData {
        val context = InstrumentationRegistry.getInstrumentation().context.assets;
        val inputStream = context.open(fileName)
        val reader = InputStreamReader(inputStream)
        return Gson().fromJson(reader, TestData::class.java)

    }
}
