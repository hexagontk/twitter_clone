import com.hexagonkt.helpers.require
import com.hexagonkt.http.server.Server
import com.hexagonkt.http.server.ServerPort
import com.hexagonkt.http.server.jetty.JettyServletAdapter
import com.hexagonkt.injection.InjectionManager
import com.hexagonkt.settings.SettingsManager.settings
import com.hexagonkt.store.Store
import com.hexagonkt.store.mongodb.MongoDbStore
import models.Message
import models.User
import routes.router
import java.util.*

internal val injector = InjectionManager.apply {
    bindObject<ServerPort>(JettyServletAdapter())
    bindObject(User::class, createUserStore())
    bindObject(Message::class, createMessageStore())
}

internal val server = Server(router = router)

internal fun createUserStore(): Store<User, String> {
    val mongodbUrl = settings.require("mongodbUrl").toString()
    val userStore = MongoDbStore(User::class, User::username, mongodbUrl)
    userStore.createIndex(true, User::email)
    return userStore
}

internal fun createMessageStore(): Store<Message, String> {
    val mongodbUrl = settings.require("mongodbUrl").toString()
    val messageStore = MongoDbStore(Message::class, Message::id, mongodbUrl)
    messageStore.createIndex(true, Message::id)
    return messageStore
}

internal fun main() {
    server.start()
}