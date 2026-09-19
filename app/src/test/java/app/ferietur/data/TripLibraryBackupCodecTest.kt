package app.ferietur.data

import app.ferietur.domain.EmployerKind
import app.ferietur.domain.FERIETUR_RULESET_VERSION
import app.ferietur.domain.HolidayWorkPlanRelation
import app.ferietur.domain.HolidayWorkPlanStatus
import app.ferietur.domain.PayingParty
import app.ferietur.domain.PlannedBlock
import app.ferietur.domain.RosterComparisonMode
import app.ferietur.domain.SavedTripDraft
import app.ferietur.domain.SavedTripDraftCodec
import app.ferietur.domain.TimeKind
import app.ferietur.domain.WeeklyBasis
import app.ferietur.domain.WeekendProfile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripLibraryBackupCodecTest {
    @Test
    fun roundTripPreservesCurrentDraftAndBackupMetadata() {
        val draft = draft("trip-one")
        val bytes = ByteArrayOutputStream().also { output ->
            TripLibraryBackupCodec.write(
                drafts = listOf(draft),
                output = output,
                appVersionName = "0.5.6-dev",
                appVersionCode = 53,
                createdAtEpochMillis = 123456789L,
            )
        }.toByteArray()

        val backup = TripLibraryBackupCodec.read(ByteArrayInputStream(bytes))

        assertEquals(TripLibraryBackupCodec.FORMAT_VERSION, backup.metadata.formatVersion)
        assertEquals(123456789L, backup.metadata.createdAtEpochMillis)
        assertEquals("0.5.6-dev", backup.metadata.appVersionName)
        assertEquals(53, backup.metadata.appVersionCode)
        assertEquals(FERIETUR_RULESET_VERSION, backup.metadata.rulesetVersion)
        assertEquals(SavedTripDraftCodec.SCHEMA_VERSION, backup.metadata.draftSchemaVersion)
        assertEquals(listOf(draft), backup.drafts)
    }

    @Test
    fun backupSupportsMultipleTripsAndStableIds() {
        val drafts = listOf(draft("a"), draft("b"))
        val bytes = ByteArrayOutputStream().also { output ->
            TripLibraryBackupCodec.write(
                drafts = drafts,
                output = output,
                appVersionName = "test",
                appVersionCode = 1,
                createdAtEpochMillis = 1L,
            )
        }.toByteArray()

        val restored = TripLibraryBackupCodec.read(ByteArrayInputStream(bytes))

        assertEquals(setOf("a", "b"), restored.drafts.map { it.id }.toSet())
        assertTrue(restored.drafts.all {
            it.holidayWorkPlanStatus == HolidayWorkPlanStatus.APPROVED_AND_TIMELY_NOTIFIED
        })
    }

    @Test
    fun backupWithPathTraversalTripIdIsRejectedBeforeAnyDraftIsReturned() {
        val encodedDraft = StringWriter().also { writer ->
            SavedTripDraftCodec.write(draft("trip-one"), writer)
        }.toString()
        assertTrue(encodedDraft.contains("id=trip-one"))
        val hostileDraft = encodedDraft.replace("id=trip-one", "id=../../evil")

        val manifest = Properties().apply {
            setProperty("format", "FERIETUR_LIBRARY_BACKUP")
            setProperty("formatVersion", TripLibraryBackupCodec.FORMAT_VERSION.toString())
            setProperty("createdAtEpochMillis", "1")
            setProperty("appVersionName", "test")
            setProperty("appVersionCode", "1")
            setProperty("rulesetVersion", FERIETUR_RULESET_VERSION)
            setProperty("draftSchemaVersion", SavedTripDraftCodec.SCHEMA_VERSION.toString())
            setProperty("draftCount", "1")
        }
        val bytes = ByteArrayOutputStream().also { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry("manifest.properties"))
                val manifestText = StringWriter().also { manifest.store(it, null) }.toString()
                zip.write(manifestText.toByteArray(StandardCharsets.UTF_8))
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("drafts/0000.properties"))
                zip.write(hostileDraft.toByteArray(StandardCharsets.UTF_8))
                zip.closeEntry()
            }
        }.toByteArray()

        val failure = runCatching {
            TripLibraryBackupCodec.read(ByteArrayInputStream(bytes))
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

    private fun draft(id: String): SavedTripDraft {
        val start = LocalDate.of(2026, 8, 11)
        return SavedTripDraft(
            id = id,
            updatedAtEpochMillis = 42L,
            screen = "TRIP_PLAN",
            title = "Backup-test",
            employerKind = EmployerKind.OSLO_KOMMUNE,
            payingParty = PayingParty.UNSPECIFIED,
            rosterComparisonMode = RosterComparisonMode.USE_NORMAL_ROSTER,
            startDate = start,
            endDate = start.plusDays(2),
            startTime = LocalTime.of(6, 0),
            endTime = LocalTime.of(23, 0),
            salaryStep = 32,
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            payslipChecked = true,
            rosterGapConfirmed = false,
            roster = emptyMap(),
            plans = mapOf(
                start to listOf(
                    PlannedBlock(
                        kind = TimeKind.ACTIVE_WORK,
                        start = LocalTime.of(7, 0),
                        end = LocalTime.of(15, 0),
                        holidayWorkPlanRelation =
                            HolidayWorkPlanRelation.WITHIN_HOLIDAY_WORK_PLAN,
                    ),
                ),
            ),
            outboundArrival = LocalDateTime.of(start, LocalTime.of(10, 0)),
            returnDeparture = LocalDateTime.of(start.plusDays(2), LocalTime.of(18, 0)),
            outboundTravelKind = TimeKind.TRAVEL_WITH_RESPONSIBILITY,
            returnTravelKind = TimeKind.TRAVEL_WITH_RESPONSIBILITY,
            settlementMode = "FULL_CALCULATION",
            settlementAmountText = "",
            settlementReason = "",
            holidayWorkPlanStatus = HolidayWorkPlanStatus.APPROVED_AND_TIMELY_NOTIFIED,
        )
    }
}
