import com.hexagonkt.http.client.HttpClient
import com.hexagonkt.http.client.HttpClientSettings
import com.hexagonkt.http.client.jetty.JettyClientAdapter
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import java.net.URL

@TestInstance(PER_CLASS)
class WebApplicationTest {

    private val hostname = "127.0.0.1"
    private val port = 2010

    @BeforeAll
    fun startServer() {
        System.setProperty("mongodbUrl", "mongodb://localhost/hexagon_twitter_clone")
        server.start()
    }

    @Test
    fun testServerStarts() {
        val settings = HttpClientSettings(baseUrl = URL("http://$hostname:$port"))
        val client = HttpClient(JettyClientAdapter(), settings).apply { start() }
        val response = client.get("/ping")
        assertEquals(200, response.status.code)
        assertEquals("pong", response.body)
    }

    @AfterAll
    fun shutdown() {
        server.stop()
    }
}
