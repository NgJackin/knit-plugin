// Example with circular dependencies for testing the circular dependency detection

@Provides
annotation class Provides

// Mock 'di' delegate for testing
val di = object {
    operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): Any {
        TODO("This is just for plugin testing")
    }
}

// Simple circular dependency example: A depends on B, B depends on A
@Provides
class ServiceA(
    @Provides val dependency: String
) {
    val serviceB: ServiceB by di // ServiceA depends on ServiceB
}

@Provides
class ServiceB(
    @Provides val config: String
) {
    val serviceA: ServiceA by di // ServiceB depends on ServiceA - CIRCULAR!
}

// More complex circular dependency: A -> B -> C -> A
@Provides
class ComponentA(
    @Provides val name: String
) {
    val componentB: ComponentB by di // A depends on B
}

@Provides
class ComponentB(
    @Provides val value: Int
) {
    val componentC: ComponentC by di // B depends on C
}

@Provides
class ComponentC(
    @Provides val enabled: Boolean
) {
    val componentA: ComponentA by di // C depends on A - CIRCULAR!
}

// Non-circular dependencies for comparison
@Provides
class Database(
    @Provides val connectionString: String
) {
    // No circular dependencies here
}

@Provides
class UserRepository(
    @Provides val cache: Map<String, Any>
) {
    val database: Database by di // Linear dependency
}

@Provides
class UserService(
    @Provides val timeout: Long
) {
    val userRepository: UserRepository by di // Linear dependency chain
}

// Self-referencing circular dependency
@Provides
class RecursiveService(
    @Provides val depth: Int
) {
    val self: RecursiveService by di // Self-dependency - CIRCULAR!
}

fun main() {
    println("Testing circular dependency detection...")
}
