
import com.hexagonkt.core.Jvm
import com.hexagonkt.http.server.HttpServer
import com.hexagonkt.http.server.jetty.JettyServletAdapter
import com.hexagonkt.store.Store
import com.hexagonkt.store.mongodb.MongoDbStore
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import models.Message
import models.User
import routes.router

internal val server by lazy { HttpServer(JettyServletAdapter(), handler = router) }

internal fun createUserStore(): Store<User, String> {
    val mongodbUrl = Jvm.systemSetting<String>("mongodbUrl")
    val userStore = MongoDbStore(User::class, User::username, mongodbUrl)
    val indexField = User::email.name
    val indexOptions = IndexOptions().unique(true).background(true).name(indexField)
    userStore.collection.createIndex(Indexes.ascending(indexField), indexOptions)
//    userStore.collection.createIndex(true, User::email.name)
//    userStore.createIndex(true, User::email)
    return userStore
}

internal fun createMessageStore(): Store<Message, String> {
    val mongodbUrl = Jvm.systemSetting<String>("mongodbUrl")
    val messageStore = MongoDbStore(Message::class, Message::id, mongodbUrl)
    val indexField = Message::id.name
    val indexOptions = IndexOptions().unique(true).background(true).name(indexField)
    messageStore.collection.createIndex(Indexes.ascending(indexField), indexOptions)
    return messageStore
}

internal fun main() {
    server.start()
}
