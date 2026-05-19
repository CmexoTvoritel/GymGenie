---
name: weight-tracking-create-workout
description: Per-set weight tracking model in the create-workout flow — PendingExercise.setWeightsKg invariants and the requiresWeight gating flag.
metadata:
  type: project
---

The "add exercise to workout" 3-step flow now supports per-set weight tracking.

**Contract:**
- `ExerciseShortResponse.requiresWeight` / `ExerciseDetailResponse.requiresWeight` come from the backend (default `false`).
- `PendingExercise.setWeightsKg: List<Double?>?` and `SimpleWorkoutExerciseItem.setWeightsKg: List<Double?>?`:
  - `null` ↔ exercise is bodyweight (or legacy plan with no weight metadata).
  - non-null ↔ size **must** equal `sets`; entries may individually be `null` to mean "set logged without weight".
- `CreateWorkoutViewModel.addExercise()` normalizes the list: trims/pads to `sets`, clamps every value into `[MIN_WEIGHT_KG, MAX_WEIGHT_KG]` (0..500 kg, step 2.5).

**Why:** Backend already persists `requiresWeight`; before this change the mobile app silently discarded weight when saving — users editing weighted plans (барбелл, гантели) lost the kilos on every save.

**How to apply:**
- New shared call sites that build `PendingExercise` / `SimpleWorkoutExerciseItem` from a weighted exercise should always pass `setWeightsKg` of length == `sets`.
- The 3-step wizard (group → exercise → config) is the canonical entry point. `WorkoutDetailViewModel.addExercise(short)` seeds defaults so quick-adds from the detail screen still produce a valid payload.
- The builder hub is **not** a numbered step; the step indicator (`WorkoutFlowStepHeader` on both platforms) only renders 1/3, 2/3, 3/3.
- `CreateWorkoutViewModel.updateExerciseAt(index, updated)` reuses the same normalization as `addExercise` and is the only sanctioned way to mutate a row from the builder. The edit surface is the config screen reopened with `prefillFrom` set and `showStepHeader = false` — CTA text flips to "Сохранить изменения". On iOS the bridged Swift call is `vm.updateExerciseAt(index:updated:)`.
- The builder row subtitle shows a compact weight summary: "X кг" when every set is uniform, "min-max кг" when the user pyramided. Skipped for bodyweight rows (`requiresWeight = false` or null/empty list).
