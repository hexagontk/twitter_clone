
import com.hexagonkt.core.Jvm
import com.hexagonkt.http.server.HttpServer
import com.hexagonkt.http.server.HttpServerPort
import com.hexagonkt.http.server.jetty.JettyServletAdapter
import com.hexagonkt.injection.Injector
import com.hexagonkt.injection.Module
import com.hexagonkt.store.Store
import com.hexagonkt.store.mongodb.MongoDbStore
import models.Message
import models.User
import routes.router

internal val injector by lazy {
    Injector(
        Module().apply {
            bindInstances<HttpServerPort>(JettyServletAdapter())
            bindInstances(User::class, createUserStore())
            bindInstances(Message::class, createMessageStore())
        }
    )
}

internal val server by lazy { HttpServer(JettyServletAdapter(), handler = router) }

internal fun createUserStore(): Store<User, String> {
    val mongodbUrl = Jvm.systemSetting<String>("mongodbUrl")
    val userStore = MongoDbStore(User::class, User::username, mongodbUrl)
//    userStore.collection.createIndex(true, User::email.name)
//    userStore.createIndex(true, User::email)
    return userStore
}

internal fun createMessageStore(): Store<Message, String> {
    val mongodbUrl = Jvm.systemSetting<String>("mongodbUrl")
    val messageStore = MongoDbStore(Message::class, Message::id, mongodbUrl)
//    userStore.collection.createIndex(true, Message::id.name)
//    messageStore.createIndex(true, Message::id)
    return messageStore
}

internal fun main() {
    server.start()
}
