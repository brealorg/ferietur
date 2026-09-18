# UX01 — arbeidsplan, turnusmal og lokal sikkerhetskopi

## Problem

EQS01 gjorde tariffklassifiseringen eksplisitt, men den første UI-en krevde at samme
feriearbeidsplanforhold ble bekreftet periode for periode. På en normal åttedagers tur kunne dette
bli en vegg av like røde feilkort. Samtidig kunne en Compose-state-race etter valg av en tidligere
brukt turnusmal skrive tilbake en gammel `RosterWorkDraft` og dermed beholde vaktkoden, men miste
klokkeslettene.

Appens interne draft-backuper var dessuten kun automatisk recovery inne i appens private storage.
Brukeren hadde ingen flyttbar lokal backup.

## UX01A — turnusmal

Valg av en tidligere brukt vakt setter kode, start og slutt atomisk. Senere felt-emisjoner
transformerer alltid den nyeste `RosterWorkDraft` i listen, ikke en gammel Compose-capture.

## UX01B — grunnturnus som startpunkt

På arbeidsplansiden kan brukeren eksplisitt velge **Legg inn grunnturnusen** når det ennå ikke er
registrert ikke-reise-arbeid. Grunnturnusen kopieres da som et redigerbart startpunkt, klippes til
turens faktiske start/slutt og eksisterende reise legges over.

Dette er kun dataregistreringshjelp. De kopierte periodene får fortsatt
`HolidayWorkPlanRelation.NOT_CLARIFIED`. Grunnturnusen blir ikke tariffmessig fasit.

## UX01C — samlet ferieplanavklaring

Uavklarte punkt-20.2-relasjoner vises som én samlet avklaring i stedet for ett stort rødt kort per
dag. Brukeren kan sette alle uavklarte relevante perioder til **i planen**, og deretter åpne bare
de faktiske avvikene og velge **utover planen**.

Uavklart ferieplanrelasjon stopper ikke lenger overgangen fra Arbeidsplan til Beregning. Motorens
fail-closed-regel står fortsatt ved lag: punkt 20.2 vises som åpen og holdes utenfor kjent total.
Endelig finalisering bruker fortsatt hele valideringssettet og kan ikke gjennomføres før forholdet
er avklart.

Andre reelle arbeidsplanfeil, inkludert uavklart vaktstatus på reise uten tilsynsansvar, fortsetter
å blokkere på Arbeidsplan.

## UX01D — lokal backup

Startsiden tilbyr **Eksporter** og **Importer** under Lokal sikkerhetskopi. Androids dokumentvelger
brukes, så appen trenger ingen generell lagringstillatelse.

Backupformat 1 er én `.ferietur`-fil (ZIP-container) med:

- versjonert manifest;
- appversjon og build;
- ruleset-versjon;
- draft-schema-versjon;
- alle lesbare `SavedTripDraft`-objekter, inkludert finaliserte snapshots/historikk.

Import validerer hele filen før lagring. Turer med samme ID erstattes som eksplisitt restore, og
den eksisterende lokale turen går først gjennom den ordinære interne backupmekanismen.

Ingen konto, nettverk eller skylagring introduseres.
