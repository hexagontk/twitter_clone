package routes

import com.hexagonkt.http.handlers.path
import com.hexagonkt.http.model.FOUND_302
import com.hexagonkt.http.model.Header
import com.hexagonkt.templates.pebble.PebbleAdapter
import com.hexagonkt.web.template
import createMessageStore
import createUserStore
import isLoggedIn
import loggedInUser
import models.Message
import models.User
import java.net.URL

val userRouter = path {

    val users = createUserStore()
    val messages = createMessageStore()

    get("/follow/{username}") {
        val username: String = pathParameters["username"] ?: error("")
        val filter = mapOf(User::username.name to username)
        val user = users.findOne(filter) ?: return@get notFound("User not found")
        users.updateOne(
            loggedInUser().username,
            mapOf("following" to loggedInUser().let { it.copy(following = it.following + user.username) })
        )
        send(FOUND_302, headers = response.headers + Header("location", "/user/$username"))
    }

    get("/unfollow/{username}") {
        val username: String = pathParameters["username"] ?: error("")
        val filter = mapOf(User::username.name to username)
        val user = users.findOne(filter) ?: return@get notFound("User not found")
        users.updateOne(
            loggedInUser().username,
            mapOf("following" to loggedInUser().let { it.copy(following = it.following - user.username) })
        )
        send(FOUND_302, headers = response.headers + Header("location", "/user/$username"))
    }

    get("/{username}") {
        val username: String = pathParameters["username"] ?: error("")
        val usersFilter = mapOf(User::username.name to username)
        val user = users.findOne(usersFilter) ?: return@get notFound("User not found")
        val messagesFilter = mapOf(Message::userId.name to user.username)
        val messageFeed = messages.findMany(messagesFilter, sort = mapOf(Message::date.name to true))
        val context = mutableMapOf(
            "isPublic" to false,
            "isLoggedIn" to isLoggedIn(),
            "user" to user.username,
            "image" to user.gravatarUrl,
            "messages" to messageFeed
        )
        if (isLoggedIn()) {
            context["isOtherProfile"] = loggedInUser().username != user.username
            context["isFollowing"] = user.username in loggedInUser().following
        }
        template(
            PebbleAdapter(),
            URL("timeline.html"),
            context = context
        )
    }
}
