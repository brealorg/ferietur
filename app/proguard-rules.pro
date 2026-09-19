# Ferietur R8 rules (BUILD02).
#
# The app uses no reflection, no serialization libraries and no JNI of its own, so the default
# Android optimize rules plus the consumer rules shipped by AndroidX/Compose/DataStore are
# expected to be sufficient. Enum valueOf()/values() used by the draft and snapshot codecs are
# kept by the default rules.
#
# Keep readable stack traces in bug reports; the mapping file is produced under
# app/build/outputs/mapping/release/ and should be archived with each release.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
