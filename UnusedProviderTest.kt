// Test file for unused provider detection

@Provides
annotation class Provides

// Mock 'di' delegate for testing
val di = object {
    operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): Any {
        TODO("This is just for plugin testing")
    }
}

// USED providers (should NOT be flagged)
@Provides
class UserService  // Used below

@Provides
class Database(
    @Provides val connectionString: String  // Used below
)

class AppController {
    val userService: UserService by di  // Uses UserService
    val connection: String by di         // Uses connectionString
}

// UNUSED providers (SHOULD be flagged with quick fixes)
@Provides
class UnusedService  // No one injects this - should be flagged

@Provides
class AnotherUnusedClass {  // No one injects this - should be flagged
    fun doSomething() = "unused"
}

class SomeOtherClass(
    @Provides val unusedParameter: Int,  // No one injects Int - should be flagged
    @Provides val anotherUnused: Double  // No one injects Double - should be flagged
) {
    @Provides
    val unusedProperty: String = "test"  // No one injects String - should be flagged
}

// Mixed case - some used, some unused
@Provides
class MixedService(
    @Provides val usedConfig: Map<String, Any>,    // Used below - should NOT be flagged
    @Provides val unusedTimeout: Long              // NOT used - should be flagged
)

class Consumer {
    val config: Map<String, Any> by di  // Uses usedConfig
    // Note: Does NOT use unusedTimeout
}

fun main() {
    println("Testing unused provider detection...")
    // The plugin should offer quick fixes for:
    // 1. Remove @Provides annotation only
    // 2. Remove entire provider (class/property)
}
