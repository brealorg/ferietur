package app.ferietur.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TariffUpdateComponentCoherenceTest {
    private val currentPackage =
        FerieturTariffs.dok25_2026_2028

    private fun currentSalaryManifest(
        effectiveFrom: LocalDate =
            OsloSalaryTable2026.effectiveFromDate,
        effectiveThrough: LocalDate =
            OsloSalaryTable2026.verifiedThroughDate,
        rulesetVersion: String =
            currentPackage.rulesetVersion,
        componentId: String =
            OsloSalaryTable2026.tableId,
    ) = TariffUpdateManifest(
        updateId = "current-salary-manifest",
        tariffPackageId = currentPackage.id,
        rulesetVersion = rulesetVersion,
        effectiveFrom = effectiveFrom,
        effectiveThrough = effectiveThrough,
        components = listOf(
            TariffUpdateComponentRef(
                kind = TariffUpdateComponentKind.SALARY_TABLE,
                id = componentId,
            ),
        ),
        sourceDocuments = emptyList(),
        verification = null,
        activation = TariffUpdateActivation.CANDIDATE,
    )

    @Test
    fun currentRuntimeRegistryContainsTypedCurrentComponents() {
        val registry =
            FerieturTariffUpdateComponents.currentRuntime

        assertNotNull(
            registry.component(
                TariffUpdateComponentKind.TARIFF_PACKAGE,
                currentPackage.id,
            ),
        )

        assertNotNull(
            registry.component(
                TariffUpdateComponentKind.RULESET,
                currentPackage.rulesetVersion,
            ),
        )

        assertNotNull(
            registry.component(
                TariffUpdateComponentKind.RATE_SET,
                FerieturTariffRates.DOK25_2026_2028_RATE_SET_ID,
            ),
        )

        assertNotNull(
            registry.component(
                TariffUpdateComponentKind.SALARY_TABLE,
                OsloSalaryTable2026.tableId,
            ),
        )

        assertNotNull(
            registry.component(
                TariffUpdateComponentKind.RULE_SOURCE_BINDINGS,
                TariffUpdateRegisteredComponents.ruleSourceBindingsId(
                    currentPackage.id,
                    currentPackage.rulesetVersion,
                ),
            ),
        )
    }

    @Test
    fun salaryOnlyManifestInsideExistingPackageIsCoherent() {
        val manifest = currentSalaryManifest()

        val issues =
            TariffUpdateComponentCoherenceValidator.validate(
                manifest,
                FerieturTariffUpdateComponents.currentRuntime,
            )

        assertTrue(issues.isEmpty())
    }

    @Test
    fun unknownTargetPackageFailsClosed() {
        val manifest = currentSalaryManifest().copy(
            tariffPackageId = "missing-package",
        )

        val issues =
            TariffUpdateComponentCoherenceValidator.validate(
                manifest,
                FerieturTariffUpdateComponents.currentRuntime,
            )

        assertTrue(
            issues.any {
                it.code ==
                    TariffUpdateCoherenceCode
                        .UNKNOWN_TARGET_TARIFF_PACKAGE
            },
        )
    }

    @Test
    fun mismatchedRulesetVersionFailsClosed() {
        val manifest = currentSalaryManifest(
            rulesetVersion = "wrong-ruleset",
        )

        val issues =
            TariffUpdateComponentCoherenceValidator.validate(
                manifest,
                FerieturTariffUpdateComponents.currentRuntime,
            )

        assertTrue(
            issues.any {
                it.code ==
                    TariffUpdateCoherenceCode
                        .RULESET_VERSION_MISMATCH
            },
        )
    }

    @Test
    fun unknownComponentFailsClosed() {
        val manifest = currentSalaryManifest(
            componentId = "unknown-salary-table",
        )

        val issues =
            TariffUpdateComponentCoherenceValidator.validate(
                manifest,
                FerieturTariffUpdateComponents.currentRuntime,
            )

        assertTrue(
            issues.any {
                it.code ==
                    TariffUpdateCoherenceCode.UNKNOWN_COMPONENT
            },
        )
    }

    @Test
    fun manifestCannotClaimComponentFromAnotherPackage() {
        val start = LocalDate.of(2030, 5, 1)
        val end = LocalDate.of(2031, 4, 30)

        fun packageComponent(
            id: String,
            rulesetVersion: String,
        ) = TariffUpdateRegisteredComponent(
            kind = TariffUpdateComponentKind.TARIFF_PACKAGE,
            id = id,
            tariffPackageId = id,
            effectiveFrom = start,
            effectiveThrough = end,
            rulesetVersion = rulesetVersion,
        )

        fun rulesetComponent(
            packageId: String,
            rulesetVersion: String,
        ) = TariffUpdateRegisteredComponent(
            kind = TariffUpdateComponentKind.RULESET,
            id = rulesetVersion,
            tariffPackageId = packageId,
            effectiveFrom = start,
            effectiveThrough = end,
            rulesetVersion = rulesetVersion,
        )

        val registry = TariffUpdateComponentRegistry(
            listOf(
                packageComponent("package-a", "2030.1"),
                rulesetComponent("package-a", "2030.1"),
                packageComponent("package-b", "2030.2"),
                rulesetComponent("package-b", "2030.2"),
                TariffUpdateRegisteredComponent(
                    kind = TariffUpdateComponentKind.SALARY_TABLE,
                    id = "salary-b",
                    tariffPackageId = "package-b",
                    effectiveFrom = start,
                    effectiveThrough = end,
                ),
            ),
        )

        val manifest = TariffUpdateManifest(
            updateId = "mismatch",
            tariffPackageId = "package-a",
            rulesetVersion = "2030.1",
            effectiveFrom = start,
            effectiveThrough = end,
            components = listOf(
                TariffUpdateComponentRef(
                    kind = TariffUpdateComponentKind.SALARY_TABLE,
                    id = "salary-b",
                ),
            ),
            sourceDocuments = emptyList(),
            verification = null,
            activation = TariffUpdateActivation.CANDIDATE,
        )

        val issues =
            TariffUpdateComponentCoherenceValidator.validate(
                manifest,
                registry,
            )

        assertTrue(
            issues.any {
                it.code ==
                    TariffUpdateCoherenceCode
                        .COMPONENT_PACKAGE_MISMATCH
            },
        )
    }

    @Test
    fun manifestCannotExtendPastTargetTariffPackage() {
        val manifest = currentSalaryManifest(
            effectiveFrom = currentPackage.effectiveFrom,
            effectiveThrough =
                currentPackage.effectiveTo.plusDays(1),
        )

        val issues =
            TariffUpdateComponentCoherenceValidator.validate(
                manifest,
                FerieturTariffUpdateComponents.currentRuntime,
            )

        assertTrue(
            issues.any {
                it.code ==
                    TariffUpdateCoherenceCode
                        .MANIFEST_OUTSIDE_TARIFF_PACKAGE
            },
        )
    }

    @Test
    fun componentMustCoverEntireManifestWindow() {
        val manifest = currentSalaryManifest(
            effectiveFrom = currentPackage.effectiveFrom,
            effectiveThrough = currentPackage.effectiveTo,
        )

        val issues =
            TariffUpdateComponentCoherenceValidator.validate(
                manifest,
                FerieturTariffUpdateComponents.currentRuntime,
            )

        assertTrue(
            issues.any {
                it.code ==
                    TariffUpdateCoherenceCode
                        .COMPONENT_DOES_NOT_COVER_MANIFEST_WINDOW
            },
        )
    }

    @Test
    fun registryRejectsOrphanComponentWithoutKnownPackage() {
        val start = LocalDate.of(2030, 5, 1)
        val end = LocalDate.of(2031, 4, 30)

        assertThrows(IllegalArgumentException::class.java) {
            TariffUpdateComponentRegistry(
                listOf(
                    TariffUpdateRegisteredComponent(
                        kind =
                            TariffUpdateComponentKind.SALARY_TABLE,
                        id = "orphan-salary",
                        tariffPackageId = "missing-package",
                        effectiveFrom = start,
                        effectiveThrough = end,
                    ),
                ),
            )
        }
    }
}
