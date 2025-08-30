// Test file for missing provider detection and quick fixes

@Provides
annotation class Provides

// Mock 'di' delegate for testing
val di = object {
    operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): Any {
        TODO("This is just for plugin testing")
    }
}

// Existing providers
@Provides
class UserService

@Provides  
class Database

// Classes that inject dependencies - some missing providers
class AppController {
    val userService: UserService by di     // ✅ Provider exists
    val database: Database by di           // ✅ Provider exists
    
    // Missing providers - should show ERROR highlighting with quick fixes:
    // 🔧 When you click on these 'by di' expressions, you should see:
    // 1. "Create @Provides class for 'AppConfig'" 
    // 2. "Add @Provides parameter for 'AppConfig' to AppController constructor"
    // 3. "Add @Provides property for 'AppConfig' to AppController"
    val config: AppConfig by di            // ❌ No provider for AppConfig
    val logger: Logger by di               // ❌ No provider for Logger  
    val cache: CacheManager by di          // ❌ No provider for CacheManager
    
    // 🔧 REFACTORING TEST: Right-click on "UserService" → "Change Dependency Type"
    // Try changing UserService → UserServiceV2 
    // Should update provider + all consumers
}

class BusinessLogic {
    val userService: UserService by di     // ✅ Provider exists
    
    // Missing providers - should offer quick fix options:
    val emailService: EmailService by di   // ❌ No EmailService provider 
    val metrics: MetricsCollector by di    // ❌ No MetricsCollector provider
    val httpClient: HttpClient by di       // ❌ No HttpClient provider
}

// 🏷️ REFACTORING TEST: Right-click on @Provides → "Change Provider Type"
@Provides
fun provideUserService(): UserService {   // Try changing return type UserService → UserServiceV2
    return UserService()                   // Should update all consumers automatically
}

// Expected Quick Fix Examples:
// 1. "Create @Provides class" will generate:
//    @Provides
//    class AppConfig {
//        // TODO: Add implementation  
//    }
//
// 2. "Add @Provides parameter to constructor" will modify the class to:
//    class AppController(@Provides val appConfigProvider: AppConfig) {
//        // existing properties...
//    }
//
// 3. "Add @Provides property" will add to the class:
//    @Provides
//    val appConfigProvider: AppConfig = AppConfig() // TODO: Provide appropriate implementation

fun main() {
    println("Testing missing provider detection + refactoring...")
    println("Expected: Red error highlighting on 'by di' expressions with missing providers")
    println("Expected: Quick fix options when clicking on the errors")
    println("Expected: Refactoring actions in right-click menu for 'by di' and @Provides")
}
