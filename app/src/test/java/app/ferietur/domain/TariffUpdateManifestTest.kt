package app.ferietur.domain

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TariffUpdateManifestTest {
    private val effectiveFrom = LocalDate.of(2030, 5, 1)
    private val effectiveThrough = LocalDate.of(2031, 4, 30)

    private val source = TariffUpdateSourceDocument(
        id = "official-source",
        label = "Syntetisk offisiell tariffkilde",
        sourceUri = "https://example.invalid/tariff.pdf",
        publishedOn = LocalDate.of(2030, 4, 15),
        sha256 = "a".repeat(64),
    )

    private val verified = TariffUpdateVerification(
        verifiedAt = LocalDateTime.of(2030, 4, 20, 12, 0),
        sourceHashesVerified = true,
        notes = "Syntetisk testverifikasjon.",
    )

    private fun component(
        id: String = "salary-2030",
        sources: List<String> = emptyList(),
    ) = TariffUpdateComponentRef(
        kind = TariffUpdateComponentKind.SALARY_TABLE,
        id = id,
        sourceDocumentIds = sources,
    )

    private fun manifest(
        updateId: String = "update-2030",
        activation: TariffUpdateActivation = TariffUpdateActivation.CANDIDATE,
        components: List<TariffUpdateComponentRef> = listOf(component()),
        sources: List<TariffUpdateSourceDocument> = emptyList(),
        verification: TariffUpdateVerification? = null,
    ) = TariffUpdateManifest(
        updateId = updateId,
        tariffPackageId = "tariff-existing-or-future",
        rulesetVersion = "2030.1",
        effectiveFrom = effectiveFrom,
        effectiveThrough = effectiveThrough,
        components = components,
        sourceDocuments = sources,
        verification = verification,
        activation = activation,
    )

    @Test
    fun candidateCanExistWithoutBecomingActive() {
        val candidate = manifest()
        val catalog = TariffUpdateManifestCatalog(listOf(candidate))

        assertEquals(candidate, catalog.forUpdateId(candidate.updateId))
        assertNull(catalog.activeForUpdateId(candidate.updateId))
        assertTrue(catalog.activeManifests().isEmpty())
    }

    @Test
    fun verifiedSourceBoundManifestCanBecomeActive() {
        val active = manifest(
            activation = TariffUpdateActivation.ACTIVE,
            components = listOf(component(sources = listOf(source.id))),
            sources = listOf(source),
            verification = verified,
        )

        val catalog = TariffUpdateManifestCatalog(listOf(active))

        assertEquals(active, catalog.activeForUpdateId(active.updateId))
        assertEquals(
            active,
            catalog.activeOwnerOf(
                TariffUpdateComponentKind.SALARY_TABLE,
                "salary-2030",
            ),
        )
    }

    @Test
    fun activeManifestWithoutVerificationFailsClosed() {
        val active = manifest(
            activation = TariffUpdateActivation.ACTIVE,
            components = listOf(component(sources = listOf(source.id))),
            sources = listOf(source),
            verification = null,
        )

        val issues = TariffUpdateManifestValidator.validate(active)

        assertTrue(
            issues.any {
                it.code == TariffUpdateValidationCode.ACTIVE_VERIFICATION_REQUIRED
            },
        )

        assertThrows(IllegalArgumentException::class.java) {
            TariffUpdateManifestCatalog(listOf(active))
        }
    }

    @Test
    fun activeManifestWithoutSourceFailsClosed() {
        val active = manifest(
            activation = TariffUpdateActivation.ACTIVE,
            verification = verified,
        )

        val issues = TariffUpdateManifestValidator.validate(active)

        assertTrue(
            issues.any {
                it.code == TariffUpdateValidationCode.ACTIVE_SOURCE_REQUIRED
            },
        )
        assertTrue(
            issues.any {
                it.code == TariffUpdateValidationCode.ACTIVE_COMPONENT_SOURCE_REQUIRED
            },
        )
    }

    @Test
    fun sourceFingerprintMustBeCanonicalSha256() {
        val invalidSource = source.copy(sha256 = "NOT-A-SHA256")

        val candidate = manifest(
            sources = listOf(invalidSource),
        )

        val issues = TariffUpdateManifestValidator.validate(candidate)

        assertTrue(
            issues.any {
                it.code == TariffUpdateValidationCode.INVALID_SOURCE_SHA256
            },
        )
    }

    @Test
    fun activeComponentCannotPointAtUnknownSource() {
        val active = manifest(
            activation = TariffUpdateActivation.ACTIVE,
            components = listOf(component(sources = listOf("missing-source"))),
            sources = listOf(source),
            verification = verified,
        )

        val issues = TariffUpdateManifestValidator.validate(active)

        assertTrue(
            issues.any {
                it.code == TariffUpdateValidationCode.UNKNOWN_COMPONENT_SOURCE
            },
        )
    }

    @Test
    fun duplicateUpdateIdsAreRejected() {
        val first = manifest()
        val second = manifest()

        assertThrows(IllegalArgumentException::class.java) {
            TariffUpdateManifestCatalog(listOf(first, second))
        }
    }

    @Test
    fun sameImmutableComponentCannotBeOwnedByTwoActiveUpdates() {
        val first = manifest(
            updateId = "update-a",
            activation = TariffUpdateActivation.ACTIVE,
            components = listOf(component(sources = listOf(source.id))),
            sources = listOf(source),
            verification = verified,
        )

        val second = manifest(
            updateId = "update-b",
            activation = TariffUpdateActivation.ACTIVE,
            components = listOf(component(sources = listOf(source.id))),
            sources = listOf(source),
            verification = verified,
        )

        assertThrows(IllegalArgumentException::class.java) {
            TariffUpdateManifestCatalog(listOf(first, second))
        }
    }

    @Test
    fun updateMayTargetExistingTariffPackageWithoutClaimingNewPackageComponent() {
        val salaryOnlyUpdate = manifest(
            activation = TariffUpdateActivation.ACTIVE,
            components = listOf(
                TariffUpdateComponentRef(
                    kind = TariffUpdateComponentKind.SALARY_TABLE,
                    id = "salary-next-period",
                    sourceDocumentIds = listOf(source.id),
                ),
            ),
            sources = listOf(source),
            verification = verified,
        )

        val issues = TariffUpdateManifestValidator.validate(salaryOnlyUpdate)

        assertTrue(issues.isEmpty())
        assertTrue(
            salaryOnlyUpdate.components.none {
                it.kind == TariffUpdateComponentKind.TARIFF_PACKAGE
            },
        )
    }
}
