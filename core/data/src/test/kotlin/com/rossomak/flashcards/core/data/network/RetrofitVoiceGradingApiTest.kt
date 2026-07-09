package com.rossomak.flashcards.core.data.network

import com.rossomak.flashcards.core.data.model.EntitlementDto
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class RetrofitVoiceGradingApiTest {

    private val service: VoiceGradingRetrofitService = mockk()

    private fun createApi(): RetrofitVoiceGradingApi = RetrofitVoiceGradingApi(service)

    private fun httpException(code: Int): HttpException =
        HttpException(Response.error<Any>(code, "".toResponseBody("text/plain".toMediaType())))

    @Test
    fun `checkEntitlement maps a 403 response to VoiceGradingEntitlementException`() = runTest {
        coEvery { service.checkEntitlement() } throws httpException(403)

        val thrown = runCatching { createApi().checkEntitlement() }.exceptionOrNull()

        (thrown is VoiceGradingEntitlementException) shouldBe true
    }

    @Test
    fun `checkEntitlement rethrows non-403 http failures unchanged`() = runTest {
        val error = httpException(500)
        coEvery { service.checkEntitlement() } throws error

        val thrown = runCatching { createApi().checkEntitlement() }.exceptionOrNull()

        thrown shouldBe error
    }

    @Test
    fun `checkEntitlement returns the dto on success`() = runTest {
        val dto = EntitlementDto(isPremium = true)
        coEvery { service.checkEntitlement() } returns dto

        val result = createApi().checkEntitlement()

        result shouldBe dto
    }
}
