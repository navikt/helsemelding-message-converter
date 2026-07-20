package no.nav.helsemelding.message.client.providerregistry

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.equality.shouldBeEqualUsingFields
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import no.nav.helsemelding.message.client.providerregistry.model.FetchingError
import no.nav.helsemelding.message.converter.createProvider
import kotlin.uuid.Uuid

class HttpProviderRegistryClientSpec : StringSpec({

    "status OK should return requested provider" {
        val providerId = Uuid.random()
        val testProvider = createProvider(providerId)

        val client = testClient { request ->
            request.method shouldBe HttpMethod.Get
            request.url.fullPath shouldBe "/api/v1/behandler/$providerId"

            respond(
                content = Json.encodeToString(testProvider),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val response = client.getBehandler(providerId)

        val provider = response.shouldBeRight()
        provider shouldBeEqualUsingFields testProvider
    }

    "status not OK should return FetchingError" {
        val client = testClient {
            respond(
                content = "Provider not found",
                status = HttpStatusCode.NotFound
            )
        }

        val response = client.getBehandler(Uuid.random())

        val error = response.shouldBeLeft()
        val fetchingError = error.shouldBeInstanceOf<FetchingError>()
        fetchingError.code shouldBe HttpStatusCode.NotFound.value
        fetchingError.message shouldBe "Provider not found"
    }
})

private fun testClient(
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
): ProviderRegistryClient = HttpProviderRegistryClient(
    providerRegistryServiceUrl = "http://localhost",
    clientProvider = {
        HttpClient(MockEngine) {
            engine { addHandler(handler) }

            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        encodeDefaults = true
                    }
                )
            }
        }
    }
)
