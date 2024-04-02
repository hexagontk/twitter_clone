package routes

import SESSION_COOKIE
import com.hexagonkt.http.handlers.path
import com.hexagonkt.http.model.FOUND_302
import com.hexagonkt.templates.pebble.PebbleAdapter
import com.hexagonkt.web.template
import createMessageStore
import createUserStore
import isLoggedIn
import loggedInUser
import models.Message
import models.User
import sessions
import java.net.URL

val userRouter = path {

    val users = createUserStore()
    val messages = createMessageStore()

    get("/follow/{username}") {
        val username: String = pathParameters["username"] ?: error("")
        val filter = mapOf(User::username.name to username)
        val user = users.findOne(filter) ?: return@get notFound("User not found")
        val loggedInUser = loggedInUser().let { it.copy(following = it.following + user.username) }
        val uuid = request.cookiesMap()[SESSION_COOKIE]?.value ?: error("$SESSION_COOKIE cookie not found")
        sessions[uuid] = loggedInUser
        users.updateOne(
            loggedInUser.username,
            mapOf("following" to loggedInUser.following)
        )
        redirect(FOUND_302, "/user/$username")
    }

    get("/unfollow/{username}") {
        val username: String = pathParameters["username"] ?: error("")
        val filter = mapOf(User::username.name to username)
        val user = users.findOne(filter) ?: return@get notFound("User not found")
        val loggedInUser = loggedInUser().let { it.copy(following = it.following - user.username) }
        val uuid = request.cookiesMap()[SESSION_COOKIE]?.value ?: error("$SESSION_COOKIE cookie not found")
        sessions[uuid] = loggedInUser
        users.updateOne(
            loggedInUser.username,
            mapOf("following" to loggedInUser.following)
        )
        redirect(FOUND_302, "/user/$username")
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
            URL("classpath:templates/timeline.html"),
            context = context
        )
    }
}
