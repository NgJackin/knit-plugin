// Test file for Knit DI dependency type refactoring features

@Provides
annotation class Provides

// Mock 'di' delegate for testing
val di = object {
    operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): Any {
        TODO("This is just for plugin testing")
    }
}

// === TEST CASE 1: Dependency Type Refactoring ===
// 🔧 Right-click on "UserService" in the 'by di' property → "Change Dependency Type"
// Expected: Dialog to change UserService → UserServiceV2
// Should update: All consumers AND the provider

@Provides
class UserService {  // This should be updated to UserServiceV2
    fun getUser() = "User from v1"
}

@Provides  
fun provideUserService(): UserService {  // Return type should be updated to UserServiceV2
    return UserService()
}

class AppController {
    val userService: UserService by di     // ✅ RIGHT-CLICK HERE → "Change Dependency Type" 
    val database: Database by di           
}

class BusinessLogic {
    val userService: UserService by di     // This should also be updated to UserServiceV2
    
    fun processUser(service: UserService) { // Function parameter should be updated too
        println("Processing with $service")
    }
}

// === TEST CASE 2: Provider Type Refactoring ===  
// 🏷️ Right-click on @Provides function → "Change Provider Type"
// Expected: Dialog to change what this provider produces
// Should update: The provider return type AND all consumers

@Provides
class Database

@Provides
fun provideEmailService(): EmailService {  // ✅ RIGHT-CLICK HERE → "Change Provider Type"
    return EmailService()                   // Change EmailService → NotificationService
}

class EmailService {
    fun sendEmail() = "Email sent"
}

class NotificationService {  // Target class for refactoring
    fun sendNotification() = "Notification sent"
}

class MailController {
    val emailService: EmailService by di   // Should be updated to NotificationService when provider is refactored
}

class AlertManager {
    val emailService: EmailService by di   // Should also be updated to NotificationService
}

// === TEST CASE 3: Complex Refactoring Examples ===

@Provides
class Logger(
    @Provides val config: LoggerConfig  // Provider parameter - can be refactored
) {
    fun log(message: String) = println("Log: $message")
}

@Provides
class LoggerConfig {
    val level = "INFO"
}

class ServiceLayer {
    val logger: Logger by di        // Consumer of Logger
    val config: LoggerConfig by di  // ✅ Try refactoring LoggerConfig → AppConfig
}

// === Expected Refactoring Behaviors ===

/* 
🔧 DEPENDENCY TYPE REFACTORING (Right-click on 'by di' property):
1. Right-click on "UserService by di" 
2. Select "Change Dependency Type"
3. Dialog: "Change dependency type from 'UserService' to: [UserServiceV2]"
4. Updates:
   - All 'by di' properties: UserService → UserServiceV2
   - Provider return types: fun provideUserService(): UserServiceV2  
   - Function parameters: fun processUser(service: UserServiceV2)
   - Provider classes: class UserServiceV2 (if applicable)

🏷️ PROVIDER TYPE REFACTORING (Right-click on @Provides element):
1. Right-click on "@Provides fun provideEmailService(): EmailService"
2. Select "Change Provider Type"  
3. Dialog: "Change provider type from 'EmailService' to: [NotificationService]"
4. Updates:
   - Provider return type: fun provideEmailService(): NotificationService
   - All consumers: val service: NotificationService by di
   - Function parameters using the old type
   - Other providers that might reference this type

⚙️ ADVANCED FEATURES:
- Handles generic types: List<UserService> → List<UserServiceV2>
- Updates constructor parameters with @Provides
- Updates class-based providers  
- Updates property-based providers
- Cross-file refactoring (project-wide updates)
- Preserves code structure and formatting
*/

fun main() {
    println("=== Knit DI Refactoring Test Cases ===")
    println("1. Right-click on 'UserService by di' → Change Dependency Type")
    println("2. Right-click on '@Provides fun provideEmailService()' → Change Provider Type")
    println("3. Verify all related code is updated automatically")
}
