package app.ferietur.data

import androidx.core.util.AtomicFile
import app.ferietur.domain.EmployerKind
import app.ferietur.domain.PayingParty
import app.ferietur.domain.RosterComparisonMode
import app.ferietur.domain.SavedTripDraft
import app.ferietur.domain.WeeklyBasis
import app.ferietur.domain.WeekendProfile
import java.io.StringWriter
import java.nio.file.Files
import java.util.Properties
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripDraftStoreTest {
    @Test
    fun failedAtomicWriteKeepsLastGoodPrimary() {
        withStore { store ->
            val first = draft("atomic", 1L, "Før")
            store.save(first)

            val atomic = AtomicFile(store.primaryFileForTest(first.id))
            val stream = atomic.startWrite()
            stream.write("partial write".toByteArray())
            atomic.failWrite(stream)

            val library = store.loadLibrary()
            assertEquals(listOf(first), library.drafts)
            assertTrue(library.issues.isEmpty())
        }
    }

    @Test
    fun abandonedAtomicWriteDoesNotReplaceLastGoodPrimary() {
        withStore { store ->
            val first = draft("aborted", 1L, "Før")
            store.save(first)

            val atomic = AtomicFile(store.primaryFileForTest(first.id))
            val stream = atomic.startWrite()
            stream.write("incomplete".toByteArray())
            stream.flush()
            stream.close() // Simulates process death after closing the FD, before finishWrite().

            val library = store.loadLibrary()
            assertEquals(listOf(first), library.drafts)
            assertTrue(library.issues.isEmpty())
        }
    }

    @Test
    fun corruptPrimaryRecoversLatestValidBackupAndRestoresPrimary() {
        withStore { store ->
            val first = draft("recover", 1L, "Før")
            val second = draft("recover", 2L, "Etter")
            store.save(first)
            store.save(second) // Creates a valid backup of "Før".

            store.primaryFileForTest(second.id).writeText("not a properties draft")

            val recovered = store.loadLibrary()
            assertEquals(listOf(first), recovered.drafts)
            assertEquals(1, recovered.issues.size)
            assertEquals(TripStorageIssueKind.RECOVERED, recovered.issues.single().kind)

            val nextRead = store.loadLibrary()
            assertEquals(listOf(first), nextRead.drafts)
            assertTrue(nextRead.issues.isEmpty())
        }
    }

    @Test
    fun corruptPrimaryWithoutBackupIsReportedInsteadOfSilentlyDisappearing() {
        withStore { store ->
            val file = store.primaryFileForTest("corrupt")
            file.parentFile?.mkdirs()
            file.writeText("broken")

            val library = store.loadLibrary()
            assertTrue(library.drafts.isEmpty())
            assertEquals(TripStorageIssueKind.CORRUPT, library.issues.single().kind)
            assertEquals("corrupt", library.issues.single().draftId)
        }
    }

    @Test
    fun unsupportedPrimaryIsReportedAndNotDowngradedFromBackup() {
        withStore { store ->
            val first = draft("future", 1L, "Backup")
            val second = draft("future", 2L, "Ny")
            store.save(first)
            store.save(second)

            store.primaryFileForTest(second.id).writeText(
                """
                schemaVersion=999
                id=future
                """.trimIndent(),
            )

            val library = store.loadLibrary()
            assertTrue(library.drafts.isEmpty())
            assertEquals(TripStorageIssueKind.UNSUPPORTED_SCHEMA, library.issues.single().kind)
            assertTrue(store.primaryFileForTest(second.id).readText().contains("schemaVersion=999"))
        }
    }


    @Test
    fun preV6PrimaryIsMigratedAndPersistedExactlyOnceAtStorageBoundary() {
        withStore { store ->
            val file = store.primaryFileForTest("legacy-v5")
            file.parentFile?.mkdirs()
            val properties = Properties().apply {
                setProperty("schemaVersion", "5")
                setProperty("id", "legacy-v5")
                setProperty("updatedAtEpochMillis", "1")
                setProperty("screen", "SUMMARY")
                setProperty("title", "Legacy")
                setProperty("employerKind", "OSLO_KOMMUNE")
                setProperty("payingParty", "UNSPECIFIED")
                setProperty("rosterComparisonMode", "DO_NOT_USE_NORMAL_ROSTER")
                setProperty("fundingMode", "VACATION_SEPARATE")
                setProperty("startDate", "2026-08-25")
                setProperty("endDate", "2026-08-26")
                setProperty("startTime", "07:00")
                setProperty("endTime", "20:00")
                setProperty("salaryStep", "32")
                setProperty("weeklyBasis", "HOURS_35_5")
                setProperty("weekendProfile", "STANDARD")
                setProperty("payslipChecked", "true")
                setProperty("rosterGapConfirmed", "false")
                setProperty("outboundArrival", "2026-08-25T10:00")
                setProperty("returnDeparture", "2026-08-26T17:00")
                setProperty("settlementMode", "FULL_CALCULATION")
                setProperty("settlementAmountText", "")
                setProperty("settlementReason", "")
            }
            file.writeText(StringWriter().also { properties.store(it, "v5") }.toString())

            val migrated = store.loadLibrary()
            assertEquals("CONTROL", migrated.drafts.single().screen)
            assertTrue(
                migrated.drafts.single().migrationHistory.contains(
                    app.ferietur.domain.SavedTripDraftMigrator.MIGRATION_V6_SCHEMA,
                ),
            )
            assertTrue(file.readText().contains("schemaVersion=10"))

            val secondRead = store.loadLibrary()
            assertEquals(migrated.drafts, secondRead.drafts)
        }
    }

    @Test
    fun replacementSaveKeepsBackupAndDeleteRemovesPrimaryAndBackups() {
        withStore { store ->
            val first = draft("backup", 1L, "Før")
            val second = draft("backup", 2L, "Etter")
            store.save(first)
            store.save(second)

            assertTrue(store.backupDirectoryForTest(first.id).listFiles().orEmpty().isNotEmpty())
            assertEquals(second, store.loadLibrary().drafts.single())

            store.delete(first.id)
            assertFalse(store.primaryFileForTest(first.id).exists())
            assertFalse(store.backupDirectoryForTest(first.id).exists())
        }
    }

    private fun withStore(block: (TripDraftStore) -> Unit) {
        val root = Files.createTempDirectory("ferietur-store-test").toFile()
        try {
            block(
                TripDraftStore(
                    directory = root.resolve("trip-drafts"),
                    backupRoot = root.resolve("trip-draft-backups"),
                    createDirectories = true,
                ),
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun hostileTripIdsCanNeverAddressPathsOutsideTheStore() {
        withStore { store ->
            val survivor = draft("survivor", 1L, "Skal overleve")
            store.save(survivor)

            listOf("..", ".", "../escape", "a/b", "a\\b", "", "x".repeat(65), "trip.one").forEach { hostile ->
                assertTrue(
                    "save må avvise ID: '$hostile'",
                    runCatching { store.save(draft(hostile, 2L, "Fiendtlig")) }.isFailure,
                )
                assertTrue(
                    "delete må avvise ID: '$hostile'",
                    runCatching { store.delete(hostile) }.isFailure,
                )
            }

            val library = store.loadLibrary()
            assertEquals(listOf(survivor), library.drafts)
            assertTrue(library.issues.isEmpty())
            assertTrue(store.primaryFileForTest("survivor").exists())
        }
    }

    private fun draft(
        id: String,
        updatedAt: Long,
        title: String,
    ): SavedTripDraft {
        val start = LocalDate.of(2026, 8, 25)
        return SavedTripDraft(
            id = id,
            updatedAtEpochMillis = updatedAt,
            screen = "TRIP",
            title = title,
            employerKind = EmployerKind.OSLO_KOMMUNE,
            payingParty = PayingParty.UNSPECIFIED,
            rosterComparisonMode = RosterComparisonMode.USE_NORMAL_ROSTER,
            startDate = start,
            endDate = start.plusDays(6),
            startTime = LocalTime.of(7, 0),
            endTime = LocalTime.of(20, 0),
            salaryStep = 32,
            weeklyBasis = WeeklyBasis.HOURS_35_5,
            weekendProfile = WeekendProfile.STANDARD,
            payslipChecked = true,
            rosterGapConfirmed = false,
            roster = emptyMap(),
            plans = emptyMap(),
            outboundArrival = LocalDateTime.of(start, LocalTime.of(11, 0)),
            returnDeparture = LocalDateTime.of(start.plusDays(6), LocalTime.of(16, 0)),
            outboundTravelKind = null,
            returnTravelKind = null,
            settlementMode = "FULL_CALCULATION",
            settlementAmountText = "",
            settlementReason = "",
        )
    }
}
