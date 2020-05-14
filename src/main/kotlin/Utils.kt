import com.hexagonkt.http.server.Call
import com.hexagonkt.http.server.Session
import com.hexagonkt.templates.pebble.PebbleAdapter
import com.hexagonkt.web.template
import models.User
import java.math.BigInteger
import java.security.MessageDigest

fun String.md5(): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(toByteArray())).toString(16).padStart(32, '0')
}

fun Session.isLoggedIn() = get("loggedInUser") != null

fun Session.loggedInUser() = get("loggedInUser") as User

fun Session.logUserIn(user: User) = set("loggedInUser", user)

fun Session.logUserOut() = remove("loggedInUser")

fun Call.showError(resource: String, errorMessage: String) =
    template(PebbleAdapter, resource, context = mapOf("errorMessage" to errorMessage))