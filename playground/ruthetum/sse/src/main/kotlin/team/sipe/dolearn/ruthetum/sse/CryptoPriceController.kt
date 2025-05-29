package team.sipe.dolearn.ruthetum.sse

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/price")
class CryptoPriceController(
    private val cryptoPriceClient: CryptoPriceClient,
) {

    @GetMapping("/recent", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun price(): Flow<Map<String, Any>> {
        return flow {
            while (true) {
                val data = mapOf(
                    "BTC" to String.format("%.0f원", cryptoPriceClient.getPrice("BTC")),
                    "ETH" to String.format("%.0f원", cryptoPriceClient.getPrice("ETH")),
                    "XRP" to String.format("%.0f원", cryptoPriceClient.getPrice("XRP")),
                    "DOGE" to String.format("%.0f원", cryptoPriceClient.getPrice("DOGE")),
                )
                emit(data)
                delay(2000)
            }
        }.flowOn(Dispatchers.VIRTUAL_THREAD)
    }
}
