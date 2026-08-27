# APPINFO01 — first-run disclaimer + About + contact

## First launch

Ferietur shows a non-dismissible Material 3 `AlertDialog` until the user presses
`Jeg har forstått`.

Copy:

**Kontroller alltid beregningen**

Ferietur is a planning/calculation aid. The app can contain errors and does not
replace checking current tariff, roster, agreements or payroll information.
The user remains responsible for quality-checking the information and result
before it is used for payment or decisions.

The acknowledgement is not framed as legal Terms acceptance.

It is persisted as a versioned integer with Preferences DataStore:
`disclaimer_ack_version = 1`.

Bumping `CURRENT_DISCLAIMER_VERSION` later will intentionally show a materially
updated warning again.

## About Ferietur

Home's info affordance now opens an actual About page containing:

- what Ferietur is;
- explicit statement that it is an independent tool and not an official Oslo
  kommune app;
- the same important disclaimer;
- privacy summary: local storage, no account, no internet access;
- configured contact email and `Send e-post`;
- rules/calculation basis;
- `BuildConfig.VERSION_NAME` and `VERSION_CODE`.

Email uses Android's `ACTION_SENDTO` + `mailto:` pattern so only email-capable
apps handle the intent. No INTERNET permission is added.

## Contact privacy

The build bootstrap asks for the contact address locally in the terminal. The
address is written only into the successor source tree; it is not embedded in
this downloadable bootstrap artifact.

## Google basis

- Material 3 AlertDialog for an interruptive important prompt.
- Preferences DataStore 1.2.1 for small persistent preference state.
- ACTION_SENDTO + mailto for composing email without attachments.

## Security baseline

Baseline is BUGFIX02R2R2 + SECURITY03 hardening.

The DataStore acknowledgement lives under app-private files/datastore. The
SECURITY03 backup/data-transfer policy excludes all app data, so this preference
is not cloud-backed up or transferred by the configured Android backup policy.
