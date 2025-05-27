package team.sipe.dolearn.ruthetum.sse

import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.util.concurrent.Executors

@Component
class CryptoPriceClient {
    private val restClient by lazy {
        RestClient.builder()
            .baseUrl(BASE_URL)
            .requestFactory(
                JdkClientHttpRequestFactory(
                    HttpClient.newBuilder()
                        .executor(Executors.newVirtualThreadPerTaskExecutor())
                        .build()
                )
            )
            .build()
    }

    fun getPrice(symbol: String): Double {
        val response = restClient
            .get()
            .uri(BASE_URL + "?" + queryForSymbol(symbol))
            .retrieve()
            .toEntity(List::class.java)

        val tradePrice = (response.body as List<*>).firstOrNull()?.let {
            (it as Map<*, *>)["tradePrice"] as Double?
        } ?: 0.0
        return tradePrice
    }

    companion object {
        private const val BASE_URL = "https://crix-api-cdn.upbit.com/v1/crix/trades/days"

        private fun queryForSymbol(symbol: String): String = "code=CRIX.UPBIT.KRW-${symbol.uppercase()}&count=1&convertingPriceUnit=KRW"
    }
}