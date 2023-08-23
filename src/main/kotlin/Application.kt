
import com.hexagonkt.converters.ConvertersManager
import com.hexagonkt.core.*
import com.hexagonkt.http.server.HttpServer
import com.hexagonkt.http.server.jetty.JettyServletAdapter
import com.hexagonkt.store.Store
import com.hexagonkt.store.mongodb.MongoDbStore
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import models.Message
import models.User
import routes.router
import java.time.LocalDateTime

internal val server by lazy { HttpServer(JettyServletAdapter(), handler = router) }

internal fun createUserStore(): Store<User, String> {
    val mongodbUrl = Jvm.systemSetting<String>("mongodbUrl")
    val userStore = MongoDbStore(User::class, User::username, mongodbUrl)
    val indexField = User::email.name
    val indexOptions = IndexOptions().unique(true).background(true).name(indexField)
    userStore.collection.createIndex(Indexes.ascending(indexField), indexOptions)

    ConvertersManager.register(User::class to Map::class) {
        fieldsMapOfNotNull(
            User::username to it.username,
            User::email to it.email,
            User::password to it.password,
            User::gravatarUrl to it.gravatarUrl,
            User::following to it.following,
        )
    }
    ConvertersManager.register(Map::class to User::class) {
        User(
            username = it.requireString(User::username),
            email = it.requireString(User::email),
            password = it.requireString(User::password),
            gravatarUrl = it.requireString(User::gravatarUrl),
            following = it.getStringsOrEmpty(User::following).toSet(),
        )
    }

    return userStore
}

internal fun createMessageStore(): Store<Message, String> {
    val mongodbUrl = Jvm.systemSetting<String>("mongodbUrl")
    val messageStore = MongoDbStore(Message::class, Message::id, mongodbUrl)
    val indexField = Message::id.name
    val indexOptions = IndexOptions().unique(true).background(true).name(indexField)
    messageStore.collection.createIndex(Indexes.ascending(indexField), indexOptions)

    ConvertersManager.register(Message::class to Map::class) {
        fieldsMapOfNotNull(
            Message::id to it.id,
            Message::userId to it.userId,
            Message::text to it.text,
            Message::date to it.date,
            Message::dateString to it.dateString,
        )
    }
    ConvertersManager.register(Map::class to Message::class) {
        Message(
            id = it.requireString(Message::id),
            userId = it.requireString(Message::userId),
            text = it.requireString(Message::text),
            date = it.requireString(Message::date).let(LocalDateTime::parse),
            dateString = it.requireString(Message::dateString),
        )
    }

    return messageStore
}

internal fun main() {
    server.start()
}
