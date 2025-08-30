// Example Knit DI usage for testing the plugin

@Provides
annotation class Provides

// Mock 'di' delegate for testing
val di = object {
    operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): Any {
        TODO("This is just for plugin testing")
    }
}

@Provides
class User(val name: String)

class UserService(
    @Provides val userName: String
) {
    val user: User by di
    val greeting: String by di
}

class DatabaseService(
    @Provides val connection: String
) {
    val userList: List<User> by di
}

@Provides
class Logger(val level: String)

class ComplexService(
    @Provides val logger: Logger,
    @Provides val config: Map<String, String>
) {
    val user: User by di // Should work
    val settings: List<String> by di // Should show warning
}

fun main() {
    val service = UserService("Alice")
    println("User: ${service.user.name}")
}
