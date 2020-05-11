import com.hexagonkt.http.client.Client
import com.hexagonkt.http.client.ahc.AhcAdapter
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WebApplicationTest {

    private val hostname = "127.0.0.1"
    private val port = 2010

    @BeforeAll
    fun startServer() {
        server.start()
    }

    @Test
    fun testServerStarts() {
        val response = Client(AhcAdapter(), endpoint = "http://$hostname:$port").get("/ping")
        assertEquals(response.status, 200)
        assertEquals(response.body, "pong")
    }

    @AfterAll
    fun shutdown() {
        server.stop()
    }
}
