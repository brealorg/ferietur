# SECURITY03 backup policy

Ferietur keeps trip, roster and pay-calculation state local.

The locked baseline already used `android:allowBackup="false"`. Current Android
documentation notes that on some Android 12+ manufacturer devices that flag can
still permit device-to-device transfer.

SECURITY03 therefore keeps `allowBackup=false` and adds:

- Android 12+: `android:dataExtractionRules="@xml/data_extraction_rules"`
- Android 11 and lower: `android:fullBackupContent="@xml/backup_rules"`

Every supported backup domain is explicitly excluded from both cloud backup and
device-to-device transfer.

No cross-platform iOS transfer block is configured because Ferietur has no
corresponding iOS bundle/team identity to declare. Android requires those
platform-specific identifiers when configuring such a transfer.

The FileProvider export directory remains cache-based (`cache/exports/`), which
is outside normal Auto Backup content.
