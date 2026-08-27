# APPINFO01R3 — Cardless About

Purpose: remove the About screen's container/card hierarchy and let typography,
spacing and standard list rows carry the structure.

Retired from About:
- blue primary-container intro Surface;
- large red error-container warning Surface;
- outlined low-surface contact container;
- standalone outlined Send e-post button.

New structure:
- plain product intro;
- inline warning row (error-tinted warning icon + title + body);
- inline privacy row;
- standard clickable Material 3 ListItem for contact;
- standard clickable Material 3 ListItem for rules;
- plain version text.

No email, disclaimer acknowledgement, DataStore, navigation, tariff,
calculation, PDF, backup or security behavior changes.

Google Android Developers describes Card as a container for a single coherent
piece of content, rather than a general-purpose screen layout container.

Base APPINFO01R2 UI SHA256:
`886135df718ec374927a604efa00bbad9b80efe0839916c2e3bd366a67bdc1e2`

APPINFO01R3 UI SHA256:
`c955038fd2e73abcbe0ba37ce0577b88be652f851c044a76a84b64bfe870b80c`
