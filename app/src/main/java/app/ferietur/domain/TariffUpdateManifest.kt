package app.ferietur.domain

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Lifecycle state for tariff-update material.
 *
 * CANDIDATE means the material may exist in source/control-plane state but must
 * not be treated as approved runtime data.
 *
 * ACTIVE means the manifest has passed the activation requirements. A5A1 does
 * not wire active manifests into the runtime catalogs; that is a later,
 * separately qualified step.
 */
enum class TariffUpdateActivation {
    CANDIDATE,
    ACTIVE,
}

enum class TariffUpdateComponentKind {
    TARIFF_PACKAGE,
    RULESET,
    RATE_SET,
    SALARY_TABLE,
    RULE_SOURCE_BINDINGS,
}

data class TariffUpdateSourceDocument(
    val id: String,
    val label: String,
    val sourceUri: String,
    val publishedOn: LocalDate,
    /**
     * Canonical lowercase SHA-256 of the exact source artifact that was
     * reviewed when the update was verified.
     */
    val sha256: String,
)

data class TariffUpdateComponentRef(
    val kind: TariffUpdateComponentKind,
    val id: String,
    /**
     * IDs from [TariffUpdateManifest.sourceDocuments] supporting this
     * component. Candidate manifests may be incomplete; ACTIVE manifests may
     * not contain an unprovenanced component.
     */
    val sourceDocumentIds: List<String> = emptyList(),
)

data class TariffUpdateVerification(
    val verifiedAt: LocalDateTime,
    val sourceHashesVerified: Boolean,
    val notes: String = "",
)

/**
 * Immutable description of one proposed tariff-data update.
 *
 * [tariffPackageId] is the package the update belongs to. It does NOT imply
 * that every update creates a new TariffPackage: a later salary table can
 * legitimately target an already existing agreement package.
 */
data class TariffUpdateManifest(
    val updateId: String,
    val tariffPackageId: String,
    val rulesetVersion: String,
    val effectiveFrom: LocalDate,
    val effectiveThrough: LocalDate,
    val components: List<TariffUpdateComponentRef>,
    val sourceDocuments: List<TariffUpdateSourceDocument>,
    val verification: TariffUpdateVerification?,
    val activation: TariffUpdateActivation,
)

enum class TariffUpdateValidationCode {
    BLANK_UPDATE_ID,
    BLANK_TARIFF_PACKAGE_ID,
    BLANK_RULESET_VERSION,
    INVALID_EFFECTIVE_RANGE,
    NO_COMPONENTS,
    BLANK_COMPONENT_ID,
    DUPLICATE_COMPONENT,
    DUPLICATE_COMPONENT_SOURCE_REFERENCE,
    DUPLICATE_SOURCE_DOCUMENT_ID,
    BLANK_SOURCE_DOCUMENT_ID,
    BLANK_SOURCE_LABEL,
    BLANK_SOURCE_URI,
    INVALID_SOURCE_SHA256,
    UNKNOWN_COMPONENT_SOURCE,
    ACTIVE_SOURCE_REQUIRED,
    ACTIVE_COMPONENT_SOURCE_REQUIRED,
    ACTIVE_VERIFICATION_REQUIRED,
    ACTIVE_SOURCE_HASH_VERIFICATION_REQUIRED,
}

data class TariffUpdateValidationIssue(
    val code: TariffUpdateValidationCode,
    val detail: String,
)

object TariffUpdateManifestValidator {
    private val canonicalSha256 = Regex("^[0-9a-f]{64}$")

    fun validate(manifest: TariffUpdateManifest): List<TariffUpdateValidationIssue> {
        val issues = mutableListOf<TariffUpdateValidationIssue>()

        fun issue(code: TariffUpdateValidationCode, detail: String) {
            issues += TariffUpdateValidationIssue(code, detail)
        }

        if (manifest.updateId.isBlank()) {
            issue(
                TariffUpdateValidationCode.BLANK_UPDATE_ID,
                "Oppdaterings-ID kan ikke være tom.",
            )
        }

        if (manifest.tariffPackageId.isBlank()) {
            issue(
                TariffUpdateValidationCode.BLANK_TARIFF_PACKAGE_ID,
                "Tariffpakke-ID kan ikke være tom.",
            )
        }

        if (manifest.rulesetVersion.isBlank()) {
            issue(
                TariffUpdateValidationCode.BLANK_RULESET_VERSION,
                "Regelversjon kan ikke være tom.",
            )
        }

        if (manifest.effectiveThrough.isBefore(manifest.effectiveFrom)) {
            issue(
                TariffUpdateValidationCode.INVALID_EFFECTIVE_RANGE,
                "Oppdateringens sluttdato er før startdato.",
            )
        }

        if (manifest.components.isEmpty()) {
            issue(
                TariffUpdateValidationCode.NO_COMPONENTS,
                "En tariffoppdatering må beskrive minst én komponent.",
            )
        }

        manifest.components
            .filter { it.id.isBlank() }
            .forEach { component ->
                issue(
                    TariffUpdateValidationCode.BLANK_COMPONENT_ID,
                    "Komponent av typen ${component.kind} mangler ID.",
                )
            }

        manifest.components
            .groupBy { it.kind to it.id }
            .filterValues { it.size > 1 }
            .keys
            .forEach { (kind, id) ->
                issue(
                    TariffUpdateValidationCode.DUPLICATE_COMPONENT,
                    "Komponenten $kind/$id er registrert flere ganger.",
                )
            }

        manifest.components.forEach { component ->
            component.sourceDocumentIds
                .groupingBy { it }
                .eachCount()
                .filterValues { it > 1 }
                .keys
                .forEach { sourceId ->
                    issue(
                        TariffUpdateValidationCode.DUPLICATE_COMPONENT_SOURCE_REFERENCE,
                        "Komponenten ${component.kind}/${component.id} peker flere ganger på kilden $sourceId.",
                    )
                }
        }

        manifest.sourceDocuments
            .groupBy { it.id }
            .filterValues { it.size > 1 }
            .keys
            .forEach { id ->
                issue(
                    TariffUpdateValidationCode.DUPLICATE_SOURCE_DOCUMENT_ID,
                    "Kildedokument-ID $id er duplisert.",
                )
            }

        manifest.sourceDocuments.forEach { source ->
            if (source.id.isBlank()) {
                issue(
                    TariffUpdateValidationCode.BLANK_SOURCE_DOCUMENT_ID,
                    "Et kildedokument mangler ID.",
                )
            }

            if (source.label.isBlank()) {
                issue(
                    TariffUpdateValidationCode.BLANK_SOURCE_LABEL,
                    "Kildedokument ${source.id} mangler etikett.",
                )
            }

            if (source.sourceUri.isBlank()) {
                issue(
                    TariffUpdateValidationCode.BLANK_SOURCE_URI,
                    "Kildedokument ${source.id} mangler kildeadresse.",
                )
            }

            if (!canonicalSha256.matches(source.sha256)) {
                issue(
                    TariffUpdateValidationCode.INVALID_SOURCE_SHA256,
                    "Kildedokument ${source.id} har ikke kanonisk lowercase SHA-256.",
                )
            }
        }

        val knownSourceIds = manifest.sourceDocuments.map { it.id }.toSet()

        manifest.components.forEach { component ->
            component.sourceDocumentIds
                .filterNot { it in knownSourceIds }
                .forEach { sourceId ->
                    issue(
                        TariffUpdateValidationCode.UNKNOWN_COMPONENT_SOURCE,
                        "Komponenten ${component.kind}/${component.id} peker på ukjent kildedokument $sourceId.",
                    )
                }
        }

        if (manifest.activation == TariffUpdateActivation.ACTIVE) {
            if (manifest.sourceDocuments.isEmpty()) {
                issue(
                    TariffUpdateValidationCode.ACTIVE_SOURCE_REQUIRED,
                    "En aktiv tariffoppdatering må ha minst ett kildedokument.",
                )
            }

            manifest.components
                .filter { it.sourceDocumentIds.isEmpty() }
                .forEach { component ->
                    issue(
                        TariffUpdateValidationCode.ACTIVE_COMPONENT_SOURCE_REQUIRED,
                        "Aktiv komponent ${component.kind}/${component.id} mangler eksplisitt kildebinding.",
                    )
                }

            val verification = manifest.verification
            if (verification == null) {
                issue(
                    TariffUpdateValidationCode.ACTIVE_VERIFICATION_REQUIRED,
                    "En aktiv tariffoppdatering må være eksplisitt verifisert.",
                )
            } else if (!verification.sourceHashesVerified) {
                issue(
                    TariffUpdateValidationCode.ACTIVE_SOURCE_HASH_VERIFICATION_REQUIRED,
                    "Kildehashene må være kontrollert før tariffoppdateringen kan være aktiv.",
                )
            }
        }

        return issues
    }

    fun requireValid(manifest: TariffUpdateManifest): TariffUpdateManifest {
        val issues = validate(manifest)
        require(issues.isEmpty()) {
            "Ugyldig tariffoppdateringsmanifest ${manifest.updateId}: " +
                issues.joinToString("; ") { "${it.code}: ${it.detail}" }
        }
        return manifest
    }
}

/**
 * Control-plane catalog.
 *
 * This intentionally does not participate in FerieturTariffResolver yet.
 * Candidate manifests remain queryable for update workflow purposes but are
 * never returned by the active lookup.
 */
class TariffUpdateManifestCatalog(
    manifests: List<TariffUpdateManifest>,
) {
    private val ordered = manifests.toList()
    private val byUpdateId = ordered.associateBy { it.updateId }
    private val active = ordered.filter {
        it.activation == TariffUpdateActivation.ACTIVE
    }

    init {
        require(byUpdateId.size == ordered.size) {
            "Duplisert tariffoppdaterings-ID."
        }

        ordered.forEach(TariffUpdateManifestValidator::requireValid)

        val activeComponentClaims = active.flatMap { manifest ->
            manifest.components.map { component ->
                Triple(component.kind, component.id, manifest.updateId)
            }
        }

        val duplicateClaims = activeComponentClaims
            .groupBy { it.first to it.second }
            .filterValues { claims -> claims.map { it.third }.distinct().size > 1 }

        require(duplicateClaims.isEmpty()) {
            val collisions = duplicateClaims.keys.joinToString { (kind, id) ->
                "$kind/$id"
            }
            "Samme immutable tariffkomponent er aktivert av flere oppdateringer: $collisions"
        }
    }

    fun forUpdateId(updateId: String): TariffUpdateManifest? =
        byUpdateId[updateId]

    /**
     * Explicit activation boundary: CANDIDATE manifests are never returned.
     */
    fun activeForUpdateId(updateId: String): TariffUpdateManifest? =
        byUpdateId[updateId]?.takeIf {
            it.activation == TariffUpdateActivation.ACTIVE
        }

    fun activeManifests(): List<TariffUpdateManifest> =
        active.toList()

    fun activeOwnerOf(
        kind: TariffUpdateComponentKind,
        componentId: String,
    ): TariffUpdateManifest? =
        active.singleOrNull { manifest ->
            manifest.components.any {
                it.kind == kind && it.id == componentId
            }
        }
}
