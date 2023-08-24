package routes

import com.hexagonkt.core.media.TEXT_PLAIN
import com.hexagonkt.http.handlers.HttpHandler
import com.hexagonkt.http.handlers.path
import com.hexagonkt.http.model.ContentType
import com.hexagonkt.http.model.FOUND_302
import com.hexagonkt.http.model.NOT_FOUND_404
import com.hexagonkt.http.server.callbacks.UrlCallback
import com.hexagonkt.templates.pebble.PebbleAdapter
import com.hexagonkt.web.template
import createMessageStore
import createUserStore
import isLoggedIn
import logUserIn
import logUserOut
import loggedInUser
import models.Message
import models.User
import showError
import java.net.URI
import java.net.URL

val router: HttpHandler by lazy {
    path {
        val users = createUserStore()
        val messages = createMessageStore()

        get("/ping") {
            ok("pong", contentType = ContentType(TEXT_PLAIN))
        }

        get("/") {
            if (!isLoggedIn())
                return@get redirect(FOUND_302, URI("/public"))

            val messageFeed = if (loggedInUser().following.isEmpty()) {
                emptyList()
            } else {
                val filter = mapOf(Message::userId.name to loggedInUser().following.toList())
                messages.findMany(filter, sort = mapOf(Message::date.name to true))
            }

            template(
                PebbleAdapter(),
                URL("classpath:templates/timeline.html"),
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
                URL("classpath:templates/timeline.html"),
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
                URL("classpath:templates/register.html"),
                context = mapOf("isLoggedIn" to isLoggedIn())
            )
        }

        post("/register") {
            val email = formParameters["email"]?.string() ?: return@post badRequest("Email is required")
            val username = formParameters["username"]?.string() ?: return@post badRequest("Username is required")
            val password = formParameters["password"]?.string() ?: return@post badRequest("Password is required")

            when {
                users.findMany(mapOf(User::email.name to email)).isNotEmpty() -> {
                    showError("classpath:templates/register.html", "User with this email already exists")
                }

                users.findMany(mapOf(User::username.name to username)).isNotEmpty() -> {
                    showError("classpath:templates/register.html", "User with this username already exists")
                }

                else -> {
                    users.insertOne(
                        User(
                            email,
                            username,
                            password
                        )
                    )
                    redirect(FOUND_302, URI("/login"))
                }
            }
        }

        get("/login") {
            template(
                PebbleAdapter(),
                URL("classpath:templates/login.html"),
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
                    showError("classpath:templates/login.html", "Incorrect credentials")
                } else {
                    logUserIn(user).redirect(FOUND_302, URI("/"))
                }
            } ?: showError(resource = "classpath:templates/login.html", errorMessage = "User not found")
        }

        get("/logout") {
            logUserOut().redirect(FOUND_302, URI("/"))
        }

        path("/user", userRouter)

        post("/message") {
            val messageContent = formParameters["message"]?.string() ?: return@post badRequest("Message is required")
            messages.insertOne(Message(userId = loggedInUser().username, text = messageContent))
            redirect(FOUND_302, URI("/public"))
        }

        after(pattern = "/*", status = NOT_FOUND_404, callback = UrlCallback(URL("classpath:public")))
    }
}
