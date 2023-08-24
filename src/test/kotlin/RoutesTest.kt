import com.hexagonkt.http.client.HttpClient
import com.hexagonkt.http.client.HttpClientSettings
import com.hexagonkt.http.client.jetty.JettyClientAdapter
import com.hexagonkt.http.model.*
import com.hexagonkt.http.model.HttpMethod.POST
import models.Message
import models.User
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import java.net.URL

@TestInstance(PER_CLASS)
class RoutesTest {
    private val hostname = "127.0.0.1"
    private val port = 2010
    private val baseUrl = URL("http://$hostname:$port")

    private val settings by lazy { HttpClientSettings(baseUrl = baseUrl, followRedirects = false) }
    private val client by lazy { HttpClient(JettyClientAdapter(), settings).apply { start() } }

    private val users by lazy { createUserStore() }
    private val messages by lazy { createMessageStore() }

    private val testUser = User("testuser@mail.com", "Test_User", "testpassword")
    private val testUser2 = User("testuser2@mail.com", "Test_User_2", "testpassword")
    private val testUser3 = User("testuser3@mail.com", "Test_User_3", "testpassword")

    private val testMessage = Message(userId = testUser.username, text = "Hello everyone!")
    private val testMessage2 = Message(userId = testUser.username, text = "How are you?")
    private val testMessage3 = Message(userId = testUser2.username, text = "Hello world!")
    private val testMessage4 = Message(userId = testUser3.username, text = "I am bored.")

    private fun performRegistration(
        email: String, username: String, password: String
    ): HttpResponsePort {

        val parts = listOf(
            HttpPart("email", email),
            HttpPart("username", username),
            HttpPart("password", password)
        )
        val request = HttpRequest(
            method = POST,
            path = "/register",
            parts = parts
        )
        return client.send(request)
    }

    private fun performLogin(email: String, password: String): HttpResponsePort {
        val parts = listOf(
            HttpPart("email", email),
            HttpPart("password", password)
        )
        val request = HttpRequest(
            method = POST,
            path = "/login",
            parts = parts
        )
        return client.send(request)
    }

    private fun sendMessage(text: String): HttpResponsePort {
        val parts = listOf(
            HttpPart("message", text)
        )
        val request = HttpRequest(
            method = POST,
            path = "/message",
            parts = parts
        )
        return client.send(request)
    }

    @BeforeAll fun startServer() {
        System.setProperty("mongodbUrl", "mongodb://localhost/hexagon_twitter_clone")
        server.start()
    }

    @Test fun testRedirectsToPublicTimelineIfNotLoggedIn() {
        val response = client.get("/")
        assertEquals(302, response.status.code)
        assertNotNull(response.headers["Location"])
        response.headers["Location"]?.let {
            assertTrue(it.values.size == 1)
            assertEquals("/public", it.value)
        }
    }

    @Test fun testRegisterPageRendersCorrectly() {
        val response = client.get("/register")
        assertEquals(200, response.status.code)
        assertNotNull(response.body)
        assertTrue(response.bodyString().contains("Register"))
    }

    @Test fun testRegistrationWorksCorrectly() {
        val response = performRegistration(
            testUser.email,
            testUser.username,
            testUser.password
        )
        assertEquals(302, response.status.code)
        assertNotNull(response.headers["Location"])
        response.headers["Location"]?.let {
            assertTrue(it.values.size == 1)
            assertEquals("/login", it.value)
        }
    }

    @Test fun testRegistrationWithDuplicateEmailReturnsError() {
        users.insertOne(testUser)
        val response = performRegistration(
            "testuser@mail.com", // Note that email is same
            "Different_Test_User", // But username is different
            "testpassword"
        )
        assertEquals(200, response.status.code)
        assertNotNull(response.body)
        assertTrue(response.bodyString().contains("User with this email already exists"))
    }

    @Test fun testRegistrationWithDuplicateUsernameReturnsError() {
        users.insertOne(testUser)
        val response = performRegistration(
            "differenttestuser@mail.com", // Note that email is different
            "Test_User", // But username is same
            "testpassword"
        )
        assertEquals(200, response.status.code)
        assertNotNull(response.body)
        assertTrue(response.bodyString().contains("User with this username already exists"))
    }

    @Test fun testLoginPageRendersCorrectly() {
        val response = client.get("/login")
        assertEquals(200, response.status.code)
        assertNotNull(response.body)
        assertTrue(response.bodyString().contains("Login"))
    }

    @Test fun testLoginWorksCorrectly() {
        users.insertOne(testUser)
        val response = performLogin(testUser.email, testUser.password)
        assertEquals(302, response.status.code)
        assertNotNull(response.headers["Location"])
        response.headers["Location"]?.let {
            assertTrue(it.values.size == 1)
            assertEquals("/", it.value)
        }
    }

    @Test fun testLoginWithInvalidEmailReturnsError() {
        users.insertOne(testUser)
        val response = performLogin("invalid@mail.com", "testpassword")
        assertEquals(200, response.status.code)
        assertNotNull(response.body)
        assertTrue(response.bodyString().contains("User not found"))
    }

    @Test fun testLoginWithInvalidPasswordReturnsError() {
        users.insertOne(testUser)
        val response = performLogin(testUser.email, "incorrectpassword")
        assertEquals(200, response.status.code)
        assertNotNull(response.body)
        assertTrue(response.bodyString().contains("Incorrect credentials"))
    }

    @Test fun testLoginButtonDisplayedWhenNotLoggedIn() {
        val response = client.get("/public")
        assertEquals(200, response.status.code)
        assertNotNull(response.body)
        response.bodyString().let {
            assertTrue(it.contains("Login"))
            assertFalse(it.contains("Logout"))
        }
    }

    @Test fun testLogoutButtonDisplayedWhenLoggedIn() {
        users.insertOne(testUser)
        performLogin(testUser.email, testUser.password)

        val response = client.get("/public")
        assertEquals(200, response.status.code)
        assertNotNull(response.body)
        response.bodyString().let {
            assertTrue(it.contains("Logout"))
            assertFalse(it.contains("Login"))
        }
    }

    @Test fun testPublicTimelineRendersCorrectlyWhenNotLoggedIn() {
        messages.insertMany(listOf(testMessage, testMessage2, testMessage3, testMessage4))

        val response = client.get("/public")
        assertEquals(200, response.status.code)
        assertNotNull(response.body)
        response.bodyString().let {
            assertTrue(it.contains("Public Timeline"))
            assertFalse(it.contains("What's on your mind?"))
            assertTrue(it.contains(testMessage.text))
            assertTrue(it.contains(testMessage2.text))
            assertTrue(it.contains(testMessage3.text))
            assertTrue(it.contains(testMessage4.text))
        }
    }

    @Test fun testPublicTimelineRendersCorrectlyWhenLoggedIn() {
        users.insertOne(testUser)
        messages.insertMany(listOf(testMessage, testMessage2, testMessage3, testMessage4))
        performLogin(testUser.email, testUser.password)

        val response = client.get("/public")
        assertEquals(200, response.status.code)
        assertNotNull(response.body)
        response.bodyString().let {
            assertTrue(it.contains("Public Timeline"))
            assertTrue(it.contains("What's on your mind?"))
            assertTrue(it.contains(testMessage.text))
            assertTrue(it.contains(testMessage2.text))
            assertTrue(it.contains(testMessage3.text))
            assertTrue(it.contains(testMessage4.text))
        }
    }

    @Test fun testMessageInsertionWorksCorrectly() {
        users.insertOne(testUser)
        performLogin(testUser.email, testUser.password)

        val response = sendMessage(testMessage.text)
        assertEquals(302, response.status.code)
        assertNotNull(response.headers["Location"])
        response.headers["Location"]?.let {
            assertTrue(it.values.size == 1)
            assertEquals("/public", it.value)
        }
        assertNotNull(
            messages.findOne(mapOf(Message::text.name to testMessage.text, User::email.name to testUser.username))
        )
    }

    @Test fun testUserPageRendersCorrectlyWhenNotLoggedIn() {
        users.insertOne(testUser)
        messages.insertMany(listOf(testMessage, testMessage2))

        val response = client.get("/user/${testUser.username}")
        assertEquals(200, response.status.code)
        assertNotNull(response.body)
        response.bodyString().let {
            assertTrue(it.contains("${testUser.username}'s Timeline"))
            assertFalse(it.contains("What's on your mind?"))
            assertTrue(it.contains(testMessage.text))
            assertTrue(it.contains(testMessage2.text))
            assertFalse(it.contains(testMessage3.text))
            assertFalse(it.contains(testMessage4.text))
        }
    }

    @Test fun testUserPageRendersCorrectlyWhenLoggedIn() {
        users.insertOne(testUser)
        messages.insertMany(listOf(testMessage, testMessage2))
        performLogin(testUser.email, testUser.password)

        val response = client.get("/user/${testUser.username}")
        assertEquals(200, response.status.code)
        assertNotNull(response.body)
        response.bodyString().let {
            assertTrue(it.contains("${testUser.username}'s Timeline"))
            assertTrue(it.contains("What's on your mind?"))
            assertTrue(it.contains(testMessage.text))
            assertTrue(it.contains(testMessage2.text))
            assertFalse(it.contains(testMessage3.text))
            assertFalse(it.contains(testMessage4.text))
        }
    }

    @Test fun testOtherUserPageRendersCorrectlyWhenLoggedIn() {
        users.insertMany(listOf(testUser, testUser2))
        messages.insertMany(listOf(testMessage, testMessage2, testMessage3))
        performLogin(testUser2.email, testUser2.password)

        val response = client.get("/user/${testUser.username}")
        assertEquals(200, response.status.code)
        assertNotNull(response.body)
        response.bodyString().let {
            assertTrue(it.contains("${testUser.username}'s Timeline"))
            assertFalse(it.contains("What's on your mind?"))
            assertTrue(it.contains(testMessage.text))
            assertTrue(it.contains(testMessage2.text))
            assertFalse(it.contains(testMessage3.text))
            assertFalse(it.contains(testMessage4.text))
        }
    }

    @Test fun testFollowWorksCorrectly() {
        users.insertMany(listOf(testUser, testUser2))
        performLogin(testUser.email, testUser.password)
        val response = client.get("/user/follow/${testUser2.username}")
        assertEquals(302, response.status.code)
        response.headers["Location"]?.let {
            assertTrue(it.values.size == 1)
            assertEquals("/user/${testUser2.username}", it.value)
        }
        users.findOne(mapOf(User::username.name to testUser.username))?.following?.contains(testUser2.username)?.let {
            assertTrue(it)
        }
    }

    @Test fun testUnfollowWorksCorrectly() {
        users.insertMany(listOf(testUser, testUser2))
        performLogin(testUser.email, testUser.password)

        client.get("/user/follow/${testUser2.username}")
        val response = client.get("/user/unfollow/${testUser2.username}")
        assertEquals(302, response.status.code)
        response.headers["Location"]?.let {
            assertTrue(it.values.size == 1)
            assertEquals("/user/${testUser2.username}", it.value)
        }
        users.findOne(mapOf(User::username.name to testUser.username))?.following?.contains(testUser2.username)?.let {
            assertFalse(it)
        }
    }

    @Test fun testLogoutWorksCorrectly() {
        users.insertOne(testUser)
        performLogin(testUser.email, testUser.password)
        val response = client.get("/logout")
        assertEquals(302, response.status.code)
        assertNotNull(response.headers["Location"])
        response.headers["Location"]?.let {
            assertTrue(it.values.size == 1)
            assertEquals("/", it.value)
        }
    }

    @AfterEach fun reset() {
        client.get("/logout")
        users.drop()
        messages.drop()
    }

    @AfterAll fun shutdown() {
        server.stop()
    }
}
