package routes

import com.mitchellbosecke.pebble.PebbleEngine
import java.io.StringWriter
import java.util.*
import kotlin.collections.HashMap

object Renderer {
    private val engine = PebbleEngine.Builder().cacheActive(true).build()

    fun render(resource: String,
               locale: Locale = Locale.getDefault(),
               context: Map<String, Any> = HashMap()): String {
        val writer = StringWriter()
        engine.getTemplate(resource).evaluate(writer, context, locale)
        return writer.toString()
    }
}