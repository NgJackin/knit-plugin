// Test example for cross-file dependency injection

// File 1: Producer
class DatabaseService {
    fun getConnection(): String = "database-connection"
}

class ApiService {
    @Provides
    fun provideDatabase(): DatabaseService = DatabaseService()
    
    @Provides
    fun provideLogger(): Logger = Logger()
}

// File 2: Consumer
class UserController {
    private val database: DatabaseService by di
    private val logger: Logger by di
    
    fun getUsers(): List<String> {
        logger.info("Getting users")
        return listOf("user1", "user2")
    }
}

// File 3: Another Consumer
class OrderController {
    private val database: DatabaseService by di
    
    fun getOrders(): List<String> {
        return listOf("order1", "order2")
    }
}

class Logger {
    fun info(message: String) {
        println("INFO: $message")
    }
}
