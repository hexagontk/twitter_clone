package routes

import com.hexagonkt.helpers.Resource
import com.hexagonkt.http.server.Router
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

val router = Router {

    val users = injector.inject<Store<User, String>>(User::class)
    val messages = injector.inject<Store<Message, String>>(Message::class)

    any("/ping") {
        ok("pong", "text/plain")
    }

    get("/") {
        if (!session.isLoggedIn()) {
            redirect("/public")
        }

        val messageFeed = if (session.loggedInUser().following.isEmpty()) {
            emptyList()
        } else {
            val filter = mapOf(Message::userId.name to session.loggedInUser().following.toList())
            messages.findMany(filter, sort = mapOf(Message::date.name to true))
        }

        template(
            PebbleAdapter,
            "timeline.html",
            context = mapOf(
                "isPublic" to false,
                "isLoggedIn" to session.isLoggedIn(),
                "user" to session.loggedInUser().username,
                "messages" to messageFeed
            )
        )
    }

    get("/public") {
        template(
            PebbleAdapter,
            "timeline.html",
            context = mapOf(
                "isPublic" to true,
                "isLoggedIn" to session.isLoggedIn(),
                "messages" to messages.findAll(sort = mapOf(Message::date.name to true))
            )
        )

    }

    get("/register") {
        template(
            PebbleAdapter,
            "register.html",
            context = mapOf(
                "isLoggedIn" to session.isLoggedIn()
            )
        )
    }

    post("/register") {
        val email = formParameters["email"] ?: halt(400, "Email is required")
        val username = formParameters["username"] ?: halt(400, "Username is required")
        val password = formParameters["password"] ?: halt(400, "Password is required")

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
                redirect("/login")
            }
        }
    }

    get("/login") {
        template(
            PebbleAdapter,
            "login.html",
            context = mapOf(
                "isLoggedIn" to session.isLoggedIn()
            )
        )
    }

    post("/login") {
        val email = formParameters["email"] ?: halt(400, "Email is required")
        val password = formParameters["password"] ?: halt(400, "Password is required")

        val filter = mapOf(User::email.name to email)
        val user = users.findOne(filter)
        user?.let {
            if (user.password != password) {
                showError("login.html", "Incorrect credentials")
            } else {
                session.logUserIn(user)
                redirect("/")
            }
        } ?: showError(resource = "login.html", errorMessage = "User not found")
    }

    get("/logout") {
        session.logUserOut()
        redirect("/")
    }

    path("/user", userRouter)

    post("/message") {
        val messageContent = formParameters["message"] ?: halt(400, "Message is required")
        messages.insertOne(Message(userId = session.loggedInUser().username, text = messageContent))
        redirect("/public")
    }

    get(Resource("public"))
}
