annotation class Provides

val di = object {
    operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): Any = TODO()
}

@Provides
class User(val name: String)

class Service {
    val user: User by di
}