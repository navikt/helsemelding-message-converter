package no.nav.helsemelding.message.client.providerregistry.model

interface ProviderError {
    val message: String
}

data class FetchingError(
    val code: Int,
    override val message: String
) : ProviderError
