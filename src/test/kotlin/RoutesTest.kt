import com.hexagonkt.http.Method
import com.hexagonkt.http.Part
import com.hexagonkt.http.Path
import com.hexagonkt.http.client.Client
import com.hexagonkt.http.client.Request
import com.hexagonkt.http.client.Response
import com.hexagonkt.http.client.ahc.AhcAdapter
import com.hexagonkt.store.Store
import models.Message
import models.User
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RoutesTest {

    private val hostname = "127.0.0.1"
    private val port = 2010

    private val client = Client(AhcAdapter(), endpoint = "http://$hostname:$port")

    private val users = injector.inject<Store<User, String>>(User::class)
    private val messages = injector.inject<Store<Message, String>>(Message::class)

    private val testUser = User("testuser@mail.com", "Test User", "testpassword")
    private val testUser2 = User("testuser2@mail.com", "Test User 2", "testpassword")
    private val testUser3 = User("testuser3@mail.com", "Test User 3", "testpassword")

    private val testMessage = Message(userId = testUser.username, text = "Hello everyone!")
    private val testMessage2 = Message(userId = testUser.username, text = "How are you?")
    private val testMessage3 = Message(userId = testUser2.username, text = "Hello world!")
    private val testMessage4 = Message(userId = testUser3.username, text = "I am bored.")

    private fun performRegistration(email: String, username: String, password: String): Response {
        val parts = mapOf(
            "email" to Part("email", email),
            "username" to Part("username", username),
            "password" to Part("password", password)
        )
        val request = Request(
            Method.POST,
            Path("/register"),
            parts = parts
        )
        return client.send(request)
    }

    private fun performLogin(email: String, password: String): Response {
        val parts = mapOf(
            "email" to Part("email", email),
            "password" to Part("password", password)
        )
        val request = Request(
            Method.POST,
            Path("/login"),
            parts = parts
        )
        return client.send(request)
    }

    private fun sendMessage(text: String): Response {
        val parts = mapOf(
            "message" to Part("message", text)
        )
        val request = Request(
            Method.POST,
            Path("/message"),
            parts = parts
        )
        return client.send(request)
    }

    @BeforeAll
    fun startServer() {
        injector
        server.start()
    }

    @Test
    fun testRedirectsToPublicTimelineIfNotLoggedIn() {
        val response = client.get("/")
        assertEquals(response.status, 302)
        assertNotNull(response.headers["Location"])
        response.headers["Location"]?.let {
            assertTrue(it.size == 1)
            assertEquals(it[0], "http://$hostname:$port/public")
        }
    }

    @Test
    fun testRegisterPageRendersCorrectly() {
        val response = client.get("/register")
        assertEquals(response.status, 200)
        assertNotNull(response.body)
        response.body?.let {
            assertTrue(it.contains("Register"))
        }
    }

    @Test
    fun testRegistrationWorksCorrectly() {
        val response = performRegistration(
            testUser.email,
            testUser.username,
            testUser.password
        )
        assertEquals(response.status, 302)
        assertNotNull(response.headers["Location"])
        response.headers["Location"]?.let {
            assertTrue(it.size == 1)
            assertEquals(it[0], "http://$hostname:$port/login")
        }
    }

    @Test
    fun testRegistrationWithDuplicateEmailReturnsError() {
        users.insertOne(testUser)
        val response = performRegistration(
            "testuser@mail.com", // Note that email is same
            "Different Test User", // But username is different
            "testpassword"
        )
        assertEquals(response.status, 200)
        assertNotNull(response.body)
        response.body?.let {
            assertTrue(it.contains("User with this email already exists"))
        }
    }

    @Test
    fun testRegistrationWithDuplicateUsernameReturnsError() {
        users.insertOne(testUser)
        val response = performRegistration(
            "differenttestuser@mail.com", // Note that email is different
            "Test User", // But username is same
            "testpassword"
        )
        assertEquals(response.status, 200)
        assertNotNull(response.body)
        response.body?.let {
            assertTrue(it.contains("User with this username already exists"))
        }
    }

    @Test
    fun testLoginPageRendersCorrectly() {
        val response = client.get("/login")
        assertEquals(response.status, 200)
        assertNotNull(response.body)
        response.body?.let {
            assertTrue(it.contains("Login"))
        }
    }

    @Test
    fun testLoginWorksCorrectly() {
        users.insertOne(testUser)
        val response = performLogin(
            testUser.email,
            testUser.password
        )
        assertEquals(response.status, 302)
        assertNotNull(response.headers["Location"])
        response.headers["Location"]?.let {
            assertTrue(it.size == 1)
            assertEquals(it[0], "http://$hostname:$port/")
        }
    }

    @Test
    fun testLoginWithInvalidEmailReturnsError() {
        users.insertOne(testUser)
        val response = performLogin(
            "invalid@mail.com",
            "testpassword"
        )
        assertEquals(response.status, 200)
        assertNotNull(response.body)
        response.body?.let {
            assertTrue(it.contains("User not found"))
        }
    }

    @Test
    fun testLoginWithInvalidPasswordReturnsError() {
        users.insertOne(testUser)
        val response = performLogin(
            testUser.email,
            "incorrectpassword"
        )
        assertEquals(response.status, 200)
        assertNotNull(response.body)
        response.body?.let {
            assertTrue(it.contains("Incorrect credentials"))
        }
    }

    @Test
    fun testLoginButtonDisplayedWhenNotLoggedIn() {
        val response = client.get("/public")
        assertEquals(response.status, 200)
        assertNotNull(response.body)
        response.body?.let {
            assertTrue(it.contains("Login"))
            assertFalse(it.contains("Logout"))
        }
    }

    @Test
    fun testLogoutButtonDisplayedWhenLoggedIn() {
        users.insertOne(testUser)
        performLogin(testUser.email, testUser.password)

        val response = client.get("/public")
        assertEquals(response.status, 200)
        assertNotNull(response.body)
        response.body?.let {
            assertTrue(it.contains("Logout"))
            assertFalse(it.contains("Login"))
        }
    }

    @Test
    fun testPublicTimelineRendersCorrectlyWhenNotLoggedIn() {
        messages.insertMany(listOf(testMessage, testMessage2, testMessage3, testMessage4))

        val response = client.get("/public")
        assertEquals(response.status, 200)
        assertNotNull(response.body)
        response.body?.let {
            assertTrue(it.contains("Public Timeline"))
            assertFalse(it.contains("What's on your mind?"))
            assertTrue(it.contains(testMessage.text))
            assertTrue(it.contains(testMessage2.text))
            assertTrue(it.contains(testMessage3.text))
            assertTrue(it.contains(testMessage4.text))
        }
    }

    @Test
    fun testPublicTimelineRendersCorrectlyWhenLoggedIn() {
        users.insertOne(testUser)
        messages.insertMany(listOf(testMessage, testMessage2, testMessage3, testMessage4))
        performLogin(testUser.email, testUser.password)

        val response = client.get("/public")
        assertEquals(response.status, 200)
        assertNotNull(response.body)
        response.body?.let {
            assertTrue(it.contains("Public Timeline"))
            assertTrue(it.contains("What's on your mind?"))
            assertTrue(it.contains(testMessage.text))
            assertTrue(it.contains(testMessage2.text))
            assertTrue(it.contains(testMessage3.text))
            assertTrue(it.contains(testMessage4.text))
        }
    }

    @Test
    fun testMessageInsertionWorksCorrectly() {
        users.insertOne(testUser)
        performLogin(testUser.email, testUser.password)

        val response = sendMessage(testMessage.text)
        assertEquals(response.status, 302)
        assertNotNull(response.headers["Location"])
        response.headers["Location"]?.let {
            assertTrue(it.size == 1)
            assertEquals(it[0], "http://$hostname:$port/public")
        }
        assertNotNull(
            messages.findOne(mapOf(Message::text.name to testMessage.text, User::email.name to testUser.username))
        )
    }

    @Test
    fun testUserPageRendersCorrectlyWhenNotLoggedIn() {
        users.insertOne(testUser)
        messages.insertMany(listOf(testMessage, testMessage2))

        val response = client.get("/user/${testUser.username}")
        assertEquals(response.status, 200)
        assertNotNull(response.body)
        response.body?.let {
            assertTrue(it.contains("${testUser.username}'s Timeline"))
            assertFalse(it.contains("What's on your mind?"))
            assertTrue(it.contains(testMessage.text))
            assertTrue(it.contains(testMessage2.text))
            assertFalse(it.contains(testMessage3.text))
            assertFalse(it.contains(testMessage4.text))
        }
    }

    @Test
    fun testUserPageRendersCorrectlyWhenLoggedIn() {
        users.insertOne(testUser)
        messages.insertMany(listOf(testMessage, testMessage2))
        performLogin(testUser.email, testUser.password)

        val response = client.get("/user/${testUser.username}")
        assertEquals(response.status, 200)
        assertNotNull(response.body)
        response.body?.let {
            assertTrue(it.contains("${testUser.username}'s Timeline"))
            assertTrue(it.contains("What's on your mind?"))
            assertTrue(it.contains(testMessage.text))
            assertTrue(it.contains(testMessage2.text))
            assertFalse(it.contains(testMessage3.text))
            assertFalse(it.contains(testMessage4.text))
        }
    }

    @Test
    fun testOtherUserPageRendersCorrectlyWhenLoggedIn() {
        users.insertMany(listOf(testUser, testUser2))
        messages.insertMany(listOf(testMessage, testMessage2, testMessage3))
        performLogin(testUser2.email, testUser2.password)

        val response = client.get("/user/${testUser.username}")
        assertEquals(response.status, 200)
        assertNotNull(response.body)
        response.body?.let {
            assertTrue(it.contains("${testUser.username}'s Timeline"))
            assertFalse(it.contains("What's on your mind?"))
            assertTrue(it.contains(testMessage.text))
            assertTrue(it.contains(testMessage2.text))
            assertFalse(it.contains(testMessage3.text))
            assertFalse(it.contains(testMessage4.text))
        }
    }

    @Test
    fun testFollowWorksCorrectly() {
        users.insertMany(listOf(testUser, testUser2))
        performLogin(testUser.email, testUser.password)
        val response = client.get("/user/follow/${testUser2.username}")
        assertEquals(response.status, 302)
        response.headers["Location"]?.let {
            assertTrue(it.size == 1)
            assertEquals(it[0], "http://$hostname:$port/user/${testUser2.username}")
        }
        users.findOne(mapOf(User::username.name to testUser.username))?.following?.contains(testUser2.username)?.let {
            assertTrue(it)
        }
    }

    @Test
    fun testUnfollowWorksCorrectly() {
        users.insertMany(listOf(testUser, testUser2))
        performLogin(testUser.email, testUser.password)

        client.get("/user/follow/${testUser2.username}")
        val response = client.get("/user/unfollow/${testUser2.username}")
        assertEquals(response.status, 302)
        response.headers["Location"]?.let {
            assertTrue(it.size == 1)
            assertEquals(it[0], "http://$hostname:$port/user/${testUser2.username}")
        }
        users.findOne(mapOf(User::username.name to testUser.username))?.following?.contains(testUser2.username)?.let {
            assertFalse(it)
        }
    }

    @Test
    fun testLogoutWorksCorrectly() {
        users.insertOne(testUser)
        performLogin(testUser.email, testUser.password)
        val response = client.get("/logout")
        assertEquals(response.status, 302)
        assertNotNull(response.headers["Location"])
        response.headers["Location"]?.let {
            assertTrue(it.size == 1)
            assertEquals(it[0], "http://$hostname:$port/")
        }
    }

    @AfterEach
    fun reset() {
        client.get("/logout")
        users.drop()
        messages.drop()
    }

    @AfterAll
    fun shutdown() {
        server.stop()
    }
}