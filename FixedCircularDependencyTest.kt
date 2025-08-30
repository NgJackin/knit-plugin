// Test case for the circular dependency fix
package knit.demo

@Provides
annotation class Provides

// Mock 'di' delegate for testing
val di = object {
    operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): Any {
        TODO("This is just for plugin testing")
    }
}

// This should NOT be detected as circular dependency
@Provides
class User(val name: String) // Producer class - name is constructor param, not injected

class UserService(
    @Provides val name: String // Producer parameter - provides String
) {
    val user: User by di // Consumer - needs User, should get it from @Provides User class
}

// This SHOULD be detected as circular dependency
@Provides
class CircularA {
    val circularB: CircularB by di // A depends on B
}

@Provides
class CircularB {
    val circularA: CircularA by di // B depends on A - CIRCULAR!
}

// Another valid non-circular case
@Provides
class Database(val connectionString: String) // Constructor param, not injected

@Provides  
class Repository(
    @Provides val cache: Map<String, Any> // Provides cache
) {
    val database: Database by di // Depends on Database - should work
}

@Provides
class Service(
    @Provides val timeout: Long // Provides timeout
) {
    val repository: Repository by di // Depends on Repository - linear chain, should work
}

fun main() {
    // This should work without circular dependency warnings:
    val userService = UserService("John")
    val user = userService.user // User("John")
    
    println("User: ${user.name}")
}
