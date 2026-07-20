package no.nav.helsemelding.message.client.scopedhttpclient.config

import java.net.URI
import kotlin.time.Duration

data class Config(
    val httpClient: HttpClient,
    val httpTokenClient: HttpClient,
    val service: Service,
    val azureAuth: AzureAuth
)

data class HttpClient(
    val connectionTimeout: Duration
)

data class Service(
    val url: URI,
    val scope: String
)

data class AzureAuth(
    val grantType: String,
    val tokenEndpoint: URI,
    val appClientId: String,
    val appClientSecret: String
)
