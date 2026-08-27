# APPINFO01R2 — checkbox + contact intent fix

Changes:

- first-run warning now has an explicit Material 3 checkbox:
  `Jeg har lest og forstått`;
- entire checkbox row is toggleable with `Role.Checkbox`;
- `Fortsett` remains disabled until checked;
- acknowledgement version bumps to 2 for the pre-release device test;
- contact email is no longer rendered on the About page;
- email button is always available;
- package-visibility-sensitive `resolveActivity()` preflight is removed;
- email launch directly calls `startActivity()` and catches
  `ActivityNotFoundException` only if launch genuinely fails;
- `ACTION_SENDTO` uses a `mailto:` URI containing the recipient.

The email destination remains embedded in the APK because a local mailto action
must know its recipient. Removing it from the UI is presentation/privacy, not
cryptographic secrecy. If the address itself must not be distributed, use a
dedicated project alias before public release.

Base UI SHA256:
`f0655a0dd2a38b471687f0500e2abd943149fbf058034f1e5e32376797470152`

R2 UI SHA256:
`886135df718ec374927a604efa00bbad9b80efe0839916c2e3bd366a67bdc1e2`

R2 preferences SHA256:
`624f88428815883a8ae3e2100f3de435528481eecf66a3ce20f7267cb2b1fb59`
