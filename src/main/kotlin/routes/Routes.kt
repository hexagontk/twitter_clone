package routes

import com.hexagonkt.core.media.TEXT_PLAIN
import com.hexagonkt.http.handlers.HttpHandler
import com.hexagonkt.http.handlers.path
import com.hexagonkt.http.model.ContentType
import com.hexagonkt.http.model.FOUND_302
import com.hexagonkt.http.model.Header
import com.hexagonkt.http.model.NOT_FOUND_404
import com.hexagonkt.http.server.callbacks.UrlCallback
import com.hexagonkt.store.Store
import com.hexagonkt.templates.pebble.PebbleAdapter
import com.hexagonkt.web.template
import injector
import isLoggedIn
import logUserIn
import logUserOut
import loggedInUser
import models.Message
import models.User
import showError
import java.net.URL

val router: HttpHandler = path {

    val users = injector.inject<Store<User, String>>(User::class)
    val messages = injector.inject<Store<Message, String>>(Message::class)

    get("/ping") {
        ok("pong", contentType = ContentType(TEXT_PLAIN))
    }

    get("/") {
        if (!isLoggedIn()) {
            return@get send(FOUND_302, headers = response.headers + Header("location", "/public"))
        }

        val messageFeed = if (loggedInUser().following.isEmpty()) {
            emptyList()
        } else {
            val filter = mapOf(Message::userId.name to loggedInUser().following.toList())
            messages.findMany(filter, sort = mapOf(Message::date.name to true))
        }

        template(
            PebbleAdapter(),
            URL("timeline.html"),
            context = mapOf(
                "isPublic" to false,
                "isLoggedIn" to isLoggedIn(),
                "user" to loggedInUser().username,
                "messages" to messageFeed
            )
        )
    }

    get("/public") {
        template(
            PebbleAdapter(),
            URL("timeline.html"),
            context = mapOf(
                "isPublic" to true,
                "isLoggedIn" to isLoggedIn(),
                "messages" to messages.findAll(sort = mapOf(Message::date.name to true))
            )
        )
    }

    get("/register") {
        template(
            PebbleAdapter(),
            URL("register.html"),
            context = mapOf("isLoggedIn" to isLoggedIn())
        )
    }

    post("/register") {
        val email = formParameters["email"]?.string() ?: return@post badRequest("Email is required")
        val username = formParameters["username"]?.string() ?: return@post badRequest("Username is required")
        val password = formParameters["password"]?.string() ?: return@post badRequest("Password is required")

        when {
            users.findMany(mapOf(User::email.name to email)).isNotEmpty() -> {
                showError("register.html", "User with this email already exists")
            }
            users.findMany(mapOf(User::username.name to username)).isNotEmpty() -> {
                showError("register.html", "User with this username already exists")
            }
            else -> {
                users.insertOne(
                    User(
                        email,
                        username,
                        password
                    )
                )
                send(FOUND_302, headers = response.headers + Header("location", "/login"))
            }
        }
    }

    get("/login") {
        template(
            PebbleAdapter(),
            URL("login.html"),
            context = mapOf("isLoggedIn" to isLoggedIn())
        )
    }

    post("/login") {
        val email = formParameters["email"]?.value ?: return@post badRequest("Email is required")
        val password = formParameters["password"]?.value ?: return@post badRequest("Password is required")

        val filter = mapOf(User::email.name to email)
        val user = users.findOne(filter)
        user?.let {
            if (user.password != password) {
                showError("login.html", "Incorrect credentials")
            } else {
                logUserIn(user)
                send(FOUND_302, headers = response.headers + Header("location", "/"))
            }
        } ?: showError(resource = "login.html", errorMessage = "User not found")
    }

    get("/logout") {
        logUserOut()
        send(FOUND_302, headers = response.headers + Header("location", "/"))
    }

    path("/user", userRouter)

    post("/message") {
        val messageContent = formParameters["message"]?.string() ?: return@post badRequest("Message is required")
        messages.insertOne(Message(userId = loggedInUser().username, text = messageContent))
        send(FOUND_302, headers = response.headers + Header("location", "/public"))
    }

    after(pattern = "/*", status = NOT_FOUND_404, callback = UrlCallback(URL("classpath:public")))
}
