// Test to verify that constructor parameters don't create false circular dependencies

@Provides
annotation class Provides

val di = object {
    operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): Any {
        TODO("This is just for plugin testing")
    }
}

// === SHOULD NOT be detected as circular dependency ===
@Provides
class User(val name: String) // Producer - name is just a constructor parameter

class UserService(
    @Provides val name: String // Producer which can provide `String`
) {
    val user: User by di // Consumer which needs a `User`
}

// === SHOULD be detected as circular dependency ===
@Provides  
class ServiceA {
    val serviceB: ServiceB by di // ServiceA depends on ServiceB
}

@Provides
class ServiceB {
    val serviceA: ServiceA by di // ServiceB depends on ServiceA - CIRCULAR!
}

// === SHOULD be detected as circular dependency (self-reference) ===
@Provides
class RecursiveService {
    val self: RecursiveService by di // Self-dependency - CIRCULAR!
}

// === Complex chain - SHOULD be detected as circular ===
@Provides
class ChainA {
    val chainB: ChainB by di
}

@Provides
class ChainB {
    val chainC: ChainC by di
}

@Provides
class ChainC {
    val chainA: ChainA by di // Completes the circle: A -> B -> C -> A
}

// === SHOULD NOT be circular (linear dependency chain) ===
@Provides
class Database(
    @Provides val connectionString: String // Just a constructor parameter provider
)

@Provides
class Repository {
    val database: Database by di // Repository depends on Database
}

@Provides  
class Service {
    val repository: Repository by di // Service depends on Repository
}

fun main() {
    println("Expected circular dependencies:")
    println("1. ServiceA ↔ ServiceB")  
    println("2. RecursiveService → RecursiveService")
    println("3. ChainA → ChainB → ChainC → ChainA")
    println()
    println("Expected NON-circular:")
    println("1. User/UserService (constructor parameter is not a dependency)")
    println("2. Database → Repository → Service (linear chain)")
}
