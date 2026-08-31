package app.ferietur.domain

import java.time.LocalDate

/**
 * A typed, immutable component visible to the tariff-update control plane.
 *
 * This registry is deliberately separate from the runtime tariff catalogs.
 * Knowing about a candidate component must not make it selectable by
 * FerieturTariffResolver.
 */
data class TariffUpdateRegisteredComponent(
    val kind: TariffUpdateComponentKind,
    val id: String,
    val tariffPackageId: String,
    val effectiveFrom: LocalDate,
    val effectiveThrough: LocalDate,
    val rulesetVersion: String? = null,
) {
    init {
        require(id.isNotBlank())
        require(tariffPackageId.isNotBlank())
        require(!effectiveThrough.isBefore(effectiveFrom)) {
            "Komponenten $kind/$id har sluttdato før startdato."
        }
    }

    fun covers(start: LocalDate, end: LocalDate): Boolean =
        !end.isBefore(start) &&
            !start.isBefore(effectiveFrom) &&
            !end.isAfter(effectiveThrough)
}

object TariffUpdateRegisteredComponents {
    fun tariffPackage(
        value: TariffPackage,
    ) = TariffUpdateRegisteredComponent(
        kind = TariffUpdateComponentKind.TARIFF_PACKAGE,
        id = value.id,
        tariffPackageId = value.id,
        effectiveFrom = value.effectiveFrom,
        effectiveThrough = value.effectiveTo,
        rulesetVersion = value.rulesetVersion,
    )

    fun ruleset(
        value: TariffPackage,
    ) = TariffUpdateRegisteredComponent(
        kind = TariffUpdateComponentKind.RULESET,
        id = value.rulesetVersion,
        tariffPackageId = value.id,
        effectiveFrom = value.effectiveFrom,
        effectiveThrough = value.effectiveTo,
        rulesetVersion = value.rulesetVersion,
    )

    fun rateSet(
        value: TariffRateSet,
    ) = TariffUpdateRegisteredComponent(
        kind = TariffUpdateComponentKind.RATE_SET,
        id = value.id,
        tariffPackageId = value.tariffPackageId,
        effectiveFrom = value.effectiveFrom,
        effectiveThrough = value.effectiveTo,
    )

    fun salaryTable(
        value: SalaryTableDescriptor,
    ) = TariffUpdateRegisteredComponent(
        kind = TariffUpdateComponentKind.SALARY_TABLE,
        id = value.id,
        tariffPackageId = value.tariffPackageId,
        effectiveFrom = value.effectiveFrom,
        effectiveThrough = value.verifiedThrough,
    )

    fun ruleSourceBindingsId(
        tariffPackageId: String,
        rulesetVersion: String,
    ): String = "$tariffPackageId-rule-sources-$rulesetVersion"

    fun ruleSourceBindings(
        tariffPackage: TariffPackage,
        bindings: List<RuleSourceBinding>,
    ): TariffUpdateRegisteredComponent {
        val tariffBindings = bindings.filter {
            it.sourceKind == RuleSourceKind.TARIFF
        }

        require(tariffBindings.isNotEmpty()) {
            "Ingen tariffregel-kilder er registrert."
        }

        require(
            tariffBindings.all {
                it.tariffPackageId == tariffPackage.id
            },
        ) {
            "Tariffregel-kildene peker ikke entydig på ${tariffPackage.id}."
        }

        return TariffUpdateRegisteredComponent(
            kind = TariffUpdateComponentKind.RULE_SOURCE_BINDINGS,
            id = ruleSourceBindingsId(
                tariffPackage.id,
                tariffPackage.rulesetVersion,
            ),
            tariffPackageId = tariffPackage.id,
            effectiveFrom = tariffPackage.effectiveFrom,
            effectiveThrough = tariffPackage.effectiveTo,
            rulesetVersion = tariffPackage.rulesetVersion,
        )
    }
}

/**
 * Registry of immutable components known to the update workflow.
 *
 * The registry itself enforces internal package coherence before a manifest
 * can be checked against it.
 */
class TariffUpdateComponentRegistry(
    components: List<TariffUpdateRegisteredComponent>,
) {
    private val ordered = components.toList()

    private val byKey = ordered.associateBy {
        it.kind to it.id
    }

    private val packages = ordered
        .filter {
            it.kind == TariffUpdateComponentKind.TARIFF_PACKAGE
        }
        .associateBy { it.id }

    init {
        require(byKey.size == ordered.size) {
            "Duplisert tariffkomponent i update-registret."
        }

        packages.values.forEach { packageComponent ->
            require(
                packageComponent.id == packageComponent.tariffPackageId,
            ) {
                "Tariffpakkekomponenten ${packageComponent.id} peker på feil pakke."
            }

            require(!packageComponent.rulesetVersion.isNullOrBlank()) {
                "Tariffpakken ${packageComponent.id} mangler ruleset-versjon."
            }
        }

        ordered
            .filter {
                it.kind != TariffUpdateComponentKind.TARIFF_PACKAGE
            }
            .forEach { component ->
                val tariffPackage = requireNotNull(
                    packages[component.tariffPackageId],
                ) {
                    "Komponenten ${component.kind}/${component.id} peker på " +
                        "ukjent tariffpakke ${component.tariffPackageId}."
                }

                require(
                    !component.effectiveFrom.isBefore(
                        tariffPackage.effectiveFrom,
                    ) &&
                        !component.effectiveThrough.isAfter(
                            tariffPackage.effectiveThrough,
                        ),
                ) {
                    "Komponenten ${component.kind}/${component.id} ligger " +
                        "utenfor tariffperioden til ${tariffPackage.id}."
                }

                if (component.kind == TariffUpdateComponentKind.RULESET) {
                    require(
                        component.id == tariffPackage.rulesetVersion,
                    ) {
                        "Ruleset-komponenten ${component.id} samsvarer ikke " +
                            "med tariffpakken ${tariffPackage.id}."
                    }
                }
            }
    }

    fun component(
        kind: TariffUpdateComponentKind,
        id: String,
    ): TariffUpdateRegisteredComponent? =
        byKey[kind to id]

    fun tariffPackage(
        id: String,
    ): TariffUpdateRegisteredComponent? =
        component(
            TariffUpdateComponentKind.TARIFF_PACKAGE,
            id,
        )

    fun all(): List<TariffUpdateRegisteredComponent> =
        ordered.toList()
}

/**
 * Current runtime components mirrored into the update control plane.
 *
 * This object is read-only metadata. It does not alter any runtime catalog.
 */
object FerieturTariffUpdateComponents {
    val currentRuntime: TariffUpdateComponentRegistry by lazy {
        val tariffPackage = FerieturTariffs.dok25_2026_2028

        val salaryDescriptor = requireNotNull(
            OsloSalaryTables.descriptorForDate(
                OsloSalaryTable2026.effectiveFromDate,
            ),
        )

        TariffUpdateComponentRegistry(
            listOf(
                TariffUpdateRegisteredComponents.tariffPackage(
                    tariffPackage,
                ),
                TariffUpdateRegisteredComponents.ruleset(
                    tariffPackage,
                ),
                TariffUpdateRegisteredComponents.rateSet(
                    FerieturTariffRates.dok25_2026_2028,
                ),
                TariffUpdateRegisteredComponents.salaryTable(
                    salaryDescriptor,
                ),
                TariffUpdateRegisteredComponents.ruleSourceBindings(
                    tariffPackage,
                    FerieturRuleSources.bindings,
                ),
            ),
        )
    }
}

enum class TariffUpdateCoherenceCode {
    UNKNOWN_TARGET_TARIFF_PACKAGE,
    RULESET_VERSION_MISMATCH,
    MANIFEST_OUTSIDE_TARIFF_PACKAGE,
    UNKNOWN_COMPONENT,
    COMPONENT_PACKAGE_MISMATCH,
    COMPONENT_DOES_NOT_COVER_MANIFEST_WINDOW,
}

data class TariffUpdateCoherenceIssue(
    val code: TariffUpdateCoherenceCode,
    val detail: String,
)

/**
 * Cross-checks a structurally valid manifest against actual known immutable
 * components.
 *
 * Structural provenance/activation validation remains the responsibility of
 * TariffUpdateManifestValidator. This validator handles component identity,
 * package ownership and effective-date coherence.
 */
object TariffUpdateComponentCoherenceValidator {
    fun validate(
        manifest: TariffUpdateManifest,
        registry: TariffUpdateComponentRegistry,
    ): List<TariffUpdateCoherenceIssue> {
        val issues = mutableListOf<TariffUpdateCoherenceIssue>()

        fun issue(
            code: TariffUpdateCoherenceCode,
            detail: String,
        ) {
            issues += TariffUpdateCoherenceIssue(code, detail)
        }

        val tariffPackage = registry.tariffPackage(
            manifest.tariffPackageId,
        )

        if (tariffPackage == null) {
            issue(
                TariffUpdateCoherenceCode.UNKNOWN_TARGET_TARIFF_PACKAGE,
                "Manifestet peker på ukjent tariffpakke " +
                    manifest.tariffPackageId,
            )
        } else {
            if (
                tariffPackage.rulesetVersion !=
                manifest.rulesetVersion
            ) {
                issue(
                    TariffUpdateCoherenceCode.RULESET_VERSION_MISMATCH,
                    "Manifestets ruleset ${manifest.rulesetVersion} samsvarer " +
                        "ikke med ${tariffPackage.rulesetVersion} i " +
                        tariffPackage.id,
                )
            }

            if (
                !tariffPackage.covers(
                    manifest.effectiveFrom,
                    manifest.effectiveThrough,
                )
            ) {
                issue(
                    TariffUpdateCoherenceCode.MANIFEST_OUTSIDE_TARIFF_PACKAGE,
                    "Manifestperioden ${manifest.effectiveFrom}–" +
                        "${manifest.effectiveThrough} ligger utenfor " +
                        "${tariffPackage.effectiveFrom}–" +
                        "${tariffPackage.effectiveThrough}.",
                )
            }
        }

        manifest.components.forEach { reference ->
            val component = registry.component(
                reference.kind,
                reference.id,
            )

            if (component == null) {
                issue(
                    TariffUpdateCoherenceCode.UNKNOWN_COMPONENT,
                    "Ukjent komponent ${reference.kind}/${reference.id}.",
                )
                return@forEach
            }

            if (
                component.tariffPackageId !=
                manifest.tariffPackageId
            ) {
                issue(
                    TariffUpdateCoherenceCode.COMPONENT_PACKAGE_MISMATCH,
                    "Komponenten ${reference.kind}/${reference.id} tilhører " +
                        "${component.tariffPackageId}, ikke " +
                        "${manifest.tariffPackageId}.",
                )
            }

            if (
                !component.covers(
                    manifest.effectiveFrom,
                    manifest.effectiveThrough,
                )
            ) {
                issue(
                    TariffUpdateCoherenceCode
                        .COMPONENT_DOES_NOT_COVER_MANIFEST_WINDOW,
                    "Komponenten ${reference.kind}/${reference.id} dekker " +
                        "${component.effectiveFrom}–" +
                        "${component.effectiveThrough}, ikke hele " +
                        "manifestperioden ${manifest.effectiveFrom}–" +
                        "${manifest.effectiveThrough}.",
                )
            }
        }

        return issues
    }

    fun requireCoherent(
        manifest: TariffUpdateManifest,
        registry: TariffUpdateComponentRegistry,
    ): TariffUpdateManifest {
        val issues = validate(manifest, registry)

        require(issues.isEmpty()) {
            "Tariffoppdateringen ${manifest.updateId} er ikke komponent-" +
                "koherent: " +
                issues.joinToString("; ") {
                    "${it.code}: ${it.detail}"
                }
        }

        return manifest
    }
}
