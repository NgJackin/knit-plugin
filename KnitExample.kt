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
class User(val name: String) // Producer class

class UserService(
    @Provides val userName: String // Producer parameter
) {
    val user: User by di // Consumer - should work (User is provided)
    val greeting: String by di // Consumer - should work (userName provides String)
}

class DatabaseService(
    @Provides val connection: String
) {
    val userList: List<User> by di // Consumer - should show warning (no List<User> provider)
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
