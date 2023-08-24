import com.hexagonkt.helpers.toBase64
import com.hexagonkt.http.handlers.HttpContext
import com.hexagonkt.http.model.Cookie
import com.hexagonkt.templates.pebble.PebbleAdapter
import com.hexagonkt.web.template
import models.User
import java.math.BigInteger
import java.net.URL
import java.security.MessageDigest
import java.util.UUID

internal val sessions: MutableMap<String, User> = mutableMapOf()
internal const val SESSION_COOKIE = "_SESSION_ID_"

fun String.md5(): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(toByteArray())).toString(16).padStart(32, '0')
}

fun HttpContext.isLoggedIn(): Boolean =
    sessions.containsKey(request.cookiesMap()[SESSION_COOKIE]?.value)

fun HttpContext.loggedInUser(): User =
    sessions[
        request.cookiesMap()[SESSION_COOKIE]?.value
            ?: error("$SESSION_COOKIE cookie not found")
    ]
    ?: error("Session not found")

fun HttpContext.logUserIn(user: User): HttpContext {
    val uuid = UUID.randomUUID().toBase64()
    sessions[uuid] = user
    return send(cookies = response.cookies + Cookie(SESSION_COOKIE, uuid))
}

fun HttpContext.logUserOut(): HttpContext {
    val c = request.cookiesMap()[SESSION_COOKIE]?.delete() ?: error("$SESSION_COOKIE cookie not found")
    sessions.remove(c.name)
    return send(cookies = response.cookies + c)
}

fun HttpContext.showError(resource: String, errorMessage: String) =
    template(PebbleAdapter(), URL(resource), context = mapOf("errorMessage" to errorMessage))
