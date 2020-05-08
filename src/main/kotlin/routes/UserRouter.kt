package routes

import com.hexagonkt.http.server.Router
import com.hexagonkt.store.Store
import com.hexagonkt.templates.pebble.PebbleAdapter
import com.hexagonkt.web.template
import injector
import isLoggedIn
import loggedInUser
import models.Message
import models.User

val userRouter = Router {

    val users = injector.inject<Store<User, String>>(User::class)
    val messages = injector.inject<Store<Message, String>>(Message::class)

    get("/follow/{username}") {
        val username: String = pathParameters["username"] ?: error("")
        val filter = mapOf(User::username.name to username)
        val user = users.findOne(filter) ?: halt(404, "User not found")
        users.updateOne(
            session.loggedInUser().username,
            mapOf("following" to session.loggedInUser().following.apply { this.add(user.username) })
        )
        redirect("/user/$username")
    }

    get("/unfollow/{username}") {
        val username: String = pathParameters["username"] ?: error("")
        val filter = mapOf(User::username.name to username)
        val user = users.findOne(filter) ?: halt(404, "User not found")
        users.updateOne(
            session.loggedInUser().username,
            mapOf("following" to session.loggedInUser().following.apply { this.remove(user.username) })
        )
        redirect("/user/$username")
    }

    get("/{username}") {
        val username: String = pathParameters["username"] ?: error("")
        val usersFilter = mapOf(User::username.name to username)
        val user = users.findOne(usersFilter) ?: halt(404, "User not found")
        val messagesFilter = mapOf(Message::userId.name to user.username)
        val messageFeed = messages.findMany(messagesFilter, sort = mapOf(Message::date.name to true))
        val context = hashMapOf(
            "isPublic" to false,
            "isLoggedIn" to session.isLoggedIn(),
            "user" to user.username,
            "image" to user.gravatarUrl,
            "messages" to messageFeed
        )
        if (session.isLoggedIn()) {
            context["isOtherProfile"] = session.loggedInUser().username != user.username
            context["isFollowing"] = user.username in session.loggedInUser().following
        }
        template(
            PebbleAdapter,
            "timeline.html",
            context = context
        )
    }
}