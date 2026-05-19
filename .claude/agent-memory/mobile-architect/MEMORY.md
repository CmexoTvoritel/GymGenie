# Mobile architect memory index

- [Koin DI integration scope](project_di_koin.md) — first phase covers only profile/network singletons; next phases planned for data and presentation modules.
- [KMP Swift naming for `init*` top-level functions](feedback_kmm_swift_naming.md) — Kotlin `initFoo()` is exposed to Swift as `doInitFoo()`; ObjC reserves the `init` prefix.
- [Keyboard dismissal in shared UI components](project_keyboard_dismissal.md) — GymGenieButton and AI flow private components already dismiss the keyboard on click; don't re-wrap at call sites.
- [Weight tracking in create-workout flow](project_weight_tracking.md) — `requiresWeight` gating + `setWeightsKg` size-equals-sets invariant; builder hub is not a numbered step.
