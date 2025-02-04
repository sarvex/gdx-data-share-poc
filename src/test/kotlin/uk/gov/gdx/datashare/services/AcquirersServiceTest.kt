package uk.gov.gdx.datashare.services

import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.repository.findByIdOrNull
import uk.gov.gdx.datashare.config.AcquirerSubscriptionNotFoundException
import uk.gov.gdx.datashare.enums.EnrichmentField
import uk.gov.gdx.datashare.enums.EventType
import uk.gov.gdx.datashare.models.AcquirerRequest
import uk.gov.gdx.datashare.models.AcquirerSubRequest
import uk.gov.gdx.datashare.repositories.*
import java.util.*

class AcquirersServiceTest {
  private val acquirerSubscriptionRepository = mockk<AcquirerSubscriptionRepository>()
  private val acquirerRepository = mockk<AcquirerRepository>()
  private val acquirerSubscriptionEnrichmentFieldRepository = mockk<AcquirerSubscriptionEnrichmentFieldRepository>()
  private val adminActionAlertsService = mockk<AdminActionAlertsService>()

  private val underTest = AcquirersService(
    acquirerSubscriptionRepository,
    acquirerRepository,
    acquirerSubscriptionEnrichmentFieldRepository,
    adminActionAlertsService,
  )

  @Test
  fun `getAcquirers gets all acquirers`() {
    val savedAcquirers = listOf(
      Acquirer(name = "Acquirer1"),
      Acquirer(name = "Acquirer2"),
      Acquirer(name = "Acquirer3"),
    )

    every { acquirerRepository.findAll() }.returns(savedAcquirers)

    val acquirers = underTest.getAcquirers().toList()

    assertThat(acquirers).hasSize(3)
    assertThat(acquirers).isEqualTo(savedAcquirers.toList())
  }

  @Test
  fun `getAcquirerSubscriptions gets all acquirer subscriptions`() {
    val savedAcquirerSubscriptions = listOf(
      AcquirerSubscription(
        acquirerId = UUID.randomUUID(),
        eventType = EventType.DEATH_NOTIFICATION,
      ),
      AcquirerSubscription(
        acquirerId = UUID.randomUUID(),
        eventType = EventType.DEATH_NOTIFICATION,
      ),
      AcquirerSubscription(
        acquirerId = UUID.randomUUID(),
        eventType = EventType.DEATH_NOTIFICATION,
      ),
    )

    val enrichmentFields = savedAcquirerSubscriptions.map {
      AcquirerSubscriptionEnrichmentField(
        acquirerSubscriptionId = it.acquirerSubscriptionId,
        enrichmentField = EnrichmentField.FIRST_NAMES,
      )
    }
    savedAcquirerSubscriptions.forEach {
      every { acquirerSubscriptionEnrichmentFieldRepository.findAllByAcquirerSubscriptionId(it.id) }.returns(
        listOf(enrichmentFields.find { enrichmentField -> enrichmentField.acquirerSubscriptionId == it.id }!!),
      )
    }

    every { acquirerSubscriptionRepository.findAll() }.returns(savedAcquirerSubscriptions)

    val acquirerSubscriptions = underTest.getAcquirerSubscriptions().toList()

    assertThat(acquirerSubscriptions).hasSize(3)
    acquirerSubscriptions.forEach {
      val savedAcquirerSubscription =
        savedAcquirerSubscriptions.find { savedAcquirerSubscription -> savedAcquirerSubscription.acquirerSubscriptionId == it.acquirerSubscriptionId }
      val savedEnrichmentFields =
        enrichmentFields.filter { enrichmentField -> enrichmentField.acquirerSubscriptionId == it.acquirerSubscriptionId }
      assertThat(it.acquirerId).isEqualTo(savedAcquirerSubscription?.acquirerId)
      assertThat(it.eventType).isEqualTo(savedAcquirerSubscription?.eventType)
      assertThat(it.enrichmentFields).isEqualTo(savedEnrichmentFields.map { enrichmentField -> enrichmentField.enrichmentField })
    }
  }

  @Test
  fun `getSubscriptionsForAcquirer gets all acquirer subscriptions for id`() {
    val savedAcquirerSubscriptions = listOf(
      AcquirerSubscription(
        acquirerId = acquirer.id,
        eventType = EventType.DEATH_NOTIFICATION,
      ),
      AcquirerSubscription(
        acquirerId = acquirer.id,
        eventType = EventType.DEATH_NOTIFICATION,
      ),
      AcquirerSubscription(
        acquirerId = acquirer.id,
        eventType = EventType.DEATH_NOTIFICATION,
      ),
    )

    val enrichmentFields = savedAcquirerSubscriptions.map {
      AcquirerSubscriptionEnrichmentField(
        acquirerSubscriptionId = it.acquirerSubscriptionId,
        enrichmentField = EnrichmentField.FIRST_NAMES,
      )
    }

    every { acquirerSubscriptionRepository.findAllByAcquirerId(acquirer.id) }.returns(savedAcquirerSubscriptions)
    savedAcquirerSubscriptions.forEach {
      every { acquirerSubscriptionEnrichmentFieldRepository.findAllByAcquirerSubscriptionId(it.id) }.returns(
        listOf(enrichmentFields.find { enrichmentField -> enrichmentField.acquirerSubscriptionId == it.id }!!),
      )
    }

    val acquirerSubscriptions = underTest.getSubscriptionsForAcquirer(acquirer.id).toList()

    assertThat(acquirerSubscriptions).hasSize(3)
    acquirerSubscriptions.forEach {
      val savedAcquirerSubscription =
        savedAcquirerSubscriptions.find { savedAcquirerSubscription -> savedAcquirerSubscription.acquirerSubscriptionId == it.acquirerSubscriptionId }
      val savedEnrichmentFields =
        enrichmentFields.filter { enrichmentField -> enrichmentField.acquirerSubscriptionId == it.acquirerSubscriptionId }
      assertThat(it.acquirerId).isEqualTo(savedAcquirerSubscription?.acquirerId)
      assertThat(it.eventType).isEqualTo(savedAcquirerSubscription?.eventType)
      assertThat(it.enrichmentFields).isEqualTo(savedEnrichmentFields.map { enrichmentField -> enrichmentField.enrichmentField })
    }
  }

  @Test
  fun `addAcquirerSubscription adds new subscription if acquirer exists`() {
    every { acquirerRepository.findByIdOrNull(acquirer.id) }.returns(acquirer)
    every { acquirerSubscriptionRepository.save(any()) }.returns(acquirerSubscription)
    every {
      acquirerSubscriptionEnrichmentFieldRepository.saveAll(any<Iterable<AcquirerSubscriptionEnrichmentField>>())
    }.returns(allEnrichmentFields)
    every { adminActionAlertsService.noticeAction(any()) } just runs

    underTest.addAcquirerSubscription(acquirer.id, acquirerSubRequest)
    verify(exactly = 1) {
      acquirerSubscriptionRepository.save(
        withArg {
          assertThat(it.acquirerId).isEqualTo(acquirer.id)
          assertThat(it.oauthClientId).isEqualTo(acquirerSubRequest.oauthClientId)
          assertThat(it.eventType).isEqualTo(acquirerSubRequest.eventType)
        },
      )
    }
    verify(exactly = 1) {
      acquirerSubscriptionEnrichmentFieldRepository.saveAll(
        withArg<Iterable<AcquirerSubscriptionEnrichmentField>> {
          assertThat(it).hasSize(2)
          assertThat(it.elementAt(0).acquirerSubscriptionId).isEqualTo(it.elementAt(1).acquirerSubscriptionId)
            .isEqualTo(acquirerSubscription.id)
          assertThat(it.map { enrichmentField -> enrichmentField.enrichmentField }).isEqualTo(
            listOf(
              EnrichmentField.FIRST_NAMES,
              EnrichmentField.LAST_NAME,
            ),
          )
        },
      )
    }
  }

  @Test
  fun `updateAcquirerSubscription updates subscription`() {
    every { acquirerRepository.findByIdOrNull(acquirer.id) }.returns(acquirer)
    every { acquirerSubscriptionRepository.findByIdOrNull(acquirerSubscription.id) }.returns(acquirerSubscription)

    every { acquirerSubscriptionRepository.save(any()) }.returns(acquirerSubscription)
    every { acquirerSubscriptionEnrichmentFieldRepository.deleteAllByAcquirerSubscriptionId(acquirerSubscription.id) }.returns(
      Unit,
    )
    every {
      acquirerSubscriptionEnrichmentFieldRepository.saveAll(any<Iterable<AcquirerSubscriptionEnrichmentField>>())
    }.returns(allEnrichmentFields)
    every { adminActionAlertsService.noticeAction(any()) } just runs

    underTest.updateAcquirerSubscription(acquirer.id, acquirerSubscription.id, acquirerSubRequest)

    verify(exactly = 1) {
      acquirerSubscriptionRepository.save(
        withArg {
          assertThat(it.acquirerId).isEqualTo(acquirer.id)
          assertThat(it.oauthClientId).isEqualTo(acquirerSubRequest.oauthClientId)
          assertThat(it.eventType).isEqualTo(acquirerSubRequest.eventType)
        },
      )
    }
    verify(exactly = 1) {
      acquirerSubscriptionEnrichmentFieldRepository.saveAll(
        withArg<Iterable<AcquirerSubscriptionEnrichmentField>> {
          assertThat(it).hasSize(2)
          assertThat(it.elementAt(0).acquirerSubscriptionId).isEqualTo(it.elementAt(1).acquirerSubscriptionId)
            .isEqualTo(acquirerSubscription.id)
          assertThat(it.map { enrichmentField -> enrichmentField.enrichmentField }).isEqualTo(
            listOf(
              EnrichmentField.FIRST_NAMES,
              EnrichmentField.LAST_NAME,
            ),
          )
        },
      )
    }
  }

  @Test
  fun `updateAcquirerSubscription does not update subscription if subscription does not exist`() {
    every { acquirerSubscriptionRepository.findByIdOrNull(acquirerSubscription.id) }.returns(null)
    every { adminActionAlertsService.noticeAction(any()) } just runs

    val exception = assertThrows<AcquirerSubscriptionNotFoundException> {
      underTest.updateAcquirerSubscription(acquirer.id, acquirerSubscription.id, acquirerSubRequest)
    }

    assertThat(exception.message).isEqualTo("Subscription ${acquirerSubscription.id} not found")

    verify(exactly = 0) { acquirerSubscriptionRepository.save(any()) }
  }

  @Test
  fun `addAcquirer adds acquirer`() {
    every { adminActionAlertsService.noticeAction(any()) } just runs
    val acquirerRequest = AcquirerRequest(
      name = "Acquirer",
    )

    every { acquirerRepository.save(any()) }.returns(acquirer)

    underTest.addAcquirer(acquirerRequest)

    verify(exactly = 1) {
      acquirerRepository.save(
        withArg {
          assertThat(it.name).isEqualTo(acquirerRequest.name)
        },
      )
    }
  }

  @Test
  fun `addAcquirer action is noticed`() {
    every { adminActionAlertsService.noticeAction(any()) } just runs
    val acquirerRequest = AcquirerRequest(
      name = "Acquirer",
    )

    every { acquirerRepository.save(any()) }.returns(acquirer)

    underTest.addAcquirer(acquirerRequest)

    verify(exactly = 1) {
      adminActionAlertsService.noticeAction(
        withArg {
          assertThat(it.name).isEqualTo("Add acquirer")
          assertThat(it.details).isEqualTo(acquirerRequest)
        },
      )
    }
  }

  @Test
  fun `addAcquirerSubscription action is noticed`() {
    every { acquirerRepository.findByIdOrNull(acquirer.id) }.returns(acquirer)
    every { acquirerSubscriptionRepository.save(any()) }.returns(acquirerSubscription)
    every {
      acquirerSubscriptionEnrichmentFieldRepository.saveAll(any<Iterable<AcquirerSubscriptionEnrichmentField>>())
    }.returns(allEnrichmentFields)
    every { adminActionAlertsService.noticeAction(any()) } just runs

    underTest.addAcquirerSubscription(acquirer.id, acquirerSubRequest)

    verify(exactly = 1) {
      adminActionAlertsService.noticeAction(
        withArg {
          assertThat(it.name).isEqualTo("Add acquirer subscription")
          assertThat(it.details.getProperty("acquirerId")).isEqualTo(acquirer.id)
          assertThat(it.details.getProperty("acquirerSubRequest")).isEqualTo(acquirerSubRequest)
        },
      )
    }
  }

  @Test
  fun `updateAcquirerSubscription action is noticed`() {
    every { acquirerRepository.findByIdOrNull(acquirer.id) }.returns(acquirer)
    every { acquirerSubscriptionRepository.findByIdOrNull(acquirerSubscription.id) }.returns(acquirerSubscription)

    every { acquirerSubscriptionRepository.save(any()) }.returns(acquirerSubscription)
    every { acquirerSubscriptionEnrichmentFieldRepository.deleteAllByAcquirerSubscriptionId(acquirerSubscription.id) }.returns(
      Unit,
    )
    every {
      acquirerSubscriptionEnrichmentFieldRepository.saveAll(any<Iterable<AcquirerSubscriptionEnrichmentField>>())
    }.returns(allEnrichmentFields)
    every { adminActionAlertsService.noticeAction(any()) } just runs

    underTest.updateAcquirerSubscription(acquirer.id, acquirerSubscription.id, acquirerSubRequest)

    verify(exactly = 1) {
      adminActionAlertsService.noticeAction(
        withArg {
          assertThat(it.name).isEqualTo("Update acquirer subscription")
          assertThat(it.details.getProperty("acquirerId")).isEqualTo(acquirer.id)
          assertThat(it.details.getProperty("subscriptionId")).isEqualTo(acquirerSubscription.id)
          assertThat(it.details.getProperty("acquirerSubRequest")).isEqualTo(acquirerSubRequest)
        },
      )
    }
  }

  private val acquirer = Acquirer(name = "Base Acquirer")
  private val acquirerSubscription = AcquirerSubscription(
    acquirerId = acquirer.id,
    oauthClientId = "pollClientId",
    eventType = EventType.DEATH_NOTIFICATION,
  )
  private val acquirerSubRequest = AcquirerSubRequest(
    eventType = EventType.LIFE_EVENT,
    enrichmentFields = listOf(EnrichmentField.FIRST_NAMES, EnrichmentField.LAST_NAME),
    oauthClientId = "callbackClientIdNew",
  )
  private val acquirerSubscriptionEnrichmentField = AcquirerSubscriptionEnrichmentField(
    acquirerSubscriptionId = acquirerSubscription.id,
    enrichmentField = EnrichmentField.FIRST_NAMES,
  )
  private val newAcquirerSubscriptionEnrichmentField = AcquirerSubscriptionEnrichmentField(
    acquirerSubscriptionId = acquirerSubscription.id,
    enrichmentField = EnrichmentField.LAST_NAME,
  )
  private val allEnrichmentFields = listOf(acquirerSubscriptionEnrichmentField, newAcquirerSubscriptionEnrichmentField)
}
