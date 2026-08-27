package app.ferietur.ui

import app.ferietur.domain.ControlFinding
import app.ferietur.domain.FindingSeverity

internal data class ControlFindingGroup(
    val title: String,
    val findings: List<ControlFinding>,
    val severity: FindingSeverity,
)

internal fun groupControlFindings(findings: List<ControlFinding>): List<ControlFindingGroup> =
    findings
        .groupBy { it.title }
        .map { (title, group) ->
            ControlFindingGroup(
                title = title,
                findings = group,
                severity = highestFindingSeverity(group),
            )
        }

internal fun highestFindingSeverity(findings: List<ControlFinding>): FindingSeverity = when {
    findings.any { it.severity == FindingSeverity.CRITICAL } -> FindingSeverity.CRITICAL
    findings.any { it.severity == FindingSeverity.OPEN } -> FindingSeverity.OPEN
    findings.any { it.severity == FindingSeverity.REVIEW } -> FindingSeverity.REVIEW
    else -> FindingSeverity.OK
}
