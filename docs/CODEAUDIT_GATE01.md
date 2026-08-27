# CODEAUDIT GATE01 — canonical active gate model

This slice changes only repository tooling/documentation. It does not mutate app production source.

Stage A of CA-012:
- archives the stale top-level SOURCE-SHA256SUMS manifest;
- archives historical phase gates out of active tools/;
- archives the old monolithic source-smoke;
- installs one current successor-semantic source contract;
- rewrites apply-build-install to use semantic gates and strict dependency verification;
- deliberately does NOT regenerate final source checksums or rewrite the final README. Those are RELEASE01/CA-012 Stage B tasks after product remediation.
