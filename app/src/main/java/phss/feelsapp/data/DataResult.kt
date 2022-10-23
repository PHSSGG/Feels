package phss.feelsapp.data

sealed class DataResult<out T : Any> {

    data class Success<out T : Any>(val data: T) : DataResult<T>()
    data class Error(val exception: Throwable) : DataResult<Nothing>()

}