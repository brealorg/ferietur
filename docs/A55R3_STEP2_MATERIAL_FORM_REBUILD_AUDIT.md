# A55R3 — Step 2 Material form rebuild

Step 2 is rebuilt structurally rather than copy-patched.

## Main surface
- two Material3 RadioButton rows for salary model
- one payment-target dropdown
- one employer field that always occupies the same location
- employer field is locked to Oslo kommune for normal rota mode
- employer dropdown becomes editable for separate-trip mode
- one compact rules ListItem; details open in AlertDialog
- legacy recovery warning remains conditional only

## Removed from Step 2
- four permanent payer cards
- conditional employer section insertion/removal
- payerScope-dependent explanatory copy
- permanent rule explanation block

No production domain, tariff, persistence, calculation or PDF semantics are changed.
