// Simple test file to verify refactoring action availability

@Provides
annotation class Provides

val di = object {
    operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): Any {
        TODO("This is just for plugin testing")
    }
}

// Test 1: Provider Type Refactoring
// Place cursor anywhere on this function and right-click
// Should see: Knit DI Refactoring → Change Provider Type
@Provides
fun provideEmailService(): EmailService {
    return EmailService()
}

class EmailService {
    fun sendEmail() = "sent"
}

// Test 2: Dependency Type Refactoring  
// Place cursor on "EmailService" in the property below and right-click
// Should see: Knit DI Refactoring → Change Dependency Type
class TestController {
    val emailService: EmailService by di  // ← Click here
}

// Test 3: Class-based provider
// Place cursor on the class name and right-click
@Provides
class DatabaseService {
    fun query() = "result"
}

class DataController {
    val db: DatabaseService by di  // ← Or click here for dependency type refactoring
}
