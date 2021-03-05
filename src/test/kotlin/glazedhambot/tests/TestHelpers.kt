package glazedhambot.tests

import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito

object TestHelpers {
    //fun <T> eq(obj: T): T = Mockito.eq<T>(obj)
    fun <T : Any> safeEq(value: T): T = eq(value) ?: value

    fun <T> any(): T = Mockito.any<T>()
}

