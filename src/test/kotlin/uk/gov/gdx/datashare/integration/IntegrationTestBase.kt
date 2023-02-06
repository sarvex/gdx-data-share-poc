package uk.gov.gdx.datashare.integration

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.gdx.datashare.TestApplication
import uk.gov.gdx.datashare.helpers.JwtAuthHelper
import uk.gov.gdx.datashare.helpers.TestBase
import uk.gov.gdx.datashare.integration.wiremock.OAuthMockServer
import uk.gov.gdx.datashare.uk.gov.gdx.datashare.integration.wiremock.PrisonerSearchApi

@SpringBootTest(webEnvironment = RANDOM_PORT, classes = [TestApplication::class])
@ActiveProfiles("test")
abstract class IntegrationTestBase : TestBase() {

  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthHelper

  init {
    // Resolves an issue where Wiremock keeps previous sockets open from other tests causing connection resets
    System.setProperty("http.keepAlive", "false")
  }

  protected fun setAuthorisation(
    user: String = "a-client",
    roles: List<String> = listOf(),
    scopes: List<String> = listOf(),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisation(user, roles, scopes)

  companion object {
    @JvmField
    val OauthMockServer = OAuthMockServer()

    @JvmField
    val PrisonerSearchApi = PrisonerSearchApi()

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      OauthMockServer.start()
      PrisonerSearchApi.start()
      OauthMockServer.stubGrantToken()
      OauthMockServer.stubOpenId()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      PrisonerSearchApi.stop()
      OauthMockServer.stop()
    }
  }
}
