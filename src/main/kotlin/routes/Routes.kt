package routes

import com.hexagonkt.helpers.Resource
import com.hexagonkt.http.server.Router
import com.hexagonkt.http.server.Session
import com.hexagonkt.store.Store
import injector
import models.Message
import models.User
import routes.Renderer.render

fun Session.isLoggedIn() = get("loggedInUser") != null

fun Session.loggedInUser() = get("loggedInUser") as User

fun Session.logUserIn(user: User) = set("loggedInUser", user)

fun Session.logUserOut() = remove("loggedInUser")

fun returnWithError(resource: String, errorMessage: String) = render(resource, context = mapOf("errorMessage" to errorMessage))

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

        ok(
            render(
                "templates/timeline.html", context = mapOf(
                    "isPublic" to false,
                    "isLoggedIn" to session.isLoggedIn(),
                    "user" to session.loggedInUser().username,
                    "messages" to messageFeed
                )
            )
        )
    }

    get("/public") {
        ok(
            render(
                "templates/timeline.html", context = mapOf(
                    "isPublic" to true,
                    "isLoggedIn" to session.isLoggedIn(),
                    "messages" to messages.findAll(sort = mapOf(Message::date.name to true))
                )
            )
        )
    }

    get("/register") {
        ok(
            render(
                "templates/register.html", context = mapOf(
                    "isLoggedIn" to session.isLoggedIn()
                )
            )
        )
    }

    post("/register") {
        val email = (formParameters["email"] ?: halt(400, "Email is required"))[0]
        val username = (formParameters["username"] ?: halt(400, "Username is required"))[0]
        val password = (formParameters["password"] ?: halt(400, "Password is required"))[0]

        if (users.findMany(mapOf(User::email.name to email)).isNotEmpty()) {
            halt(400, returnWithError("templates/register.html", "User with this email already exists"))
        }

        if (users.findMany(mapOf(User::username.name to username)).isNotEmpty()) {
            halt(400, returnWithError("templates/register.html", "User with this username already exists"))
        }

        users.insertOne(
            User(
                email,
                username,
                password
            )
        )
        redirect("/login")
    }

    get("/login") {
        ok(
            render(
                "templates/login.html", context = mapOf(
                    "isLoggedIn" to session.isLoggedIn()
                )
            )
        )
    }

    post("/login") {
        val email = (formParameters["email"] ?: halt(400, "Email is required"))[0]
        val password = (formParameters["password"] ?: halt(400, "Password is required"))[0]

        val filter = mapOf(User::email.name to email)
        val user = users.findOne(filter) ?: halt(404, returnWithError(resource = "templates/login.html", errorMessage = "User not found"))
        if (user.password != password) {
            halt(401, returnWithError("templates/login.html", "Incorrect credentials"))
        }

        session.logUserIn(user)
        redirect("/")
    }

    get("/logout") {
        session.logUserOut()
        redirect("/")
    }

    path("/user", userRouter)

    post("/message") {
        val messageContent = (formParameters["message"] ?: halt(400, "Message is required"))[0]
        messages.insertOne(
            Message(
                (messages.findAll().map { it.id }.max() ?: 0) + 1,
                session.loggedInUser().username, messageContent
            )
        )
        redirect("/")
    }

    get(Resource("public"))
}
