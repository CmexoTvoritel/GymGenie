---
name: project_icon_replacement
description: Icon replacement task outcome — emoji/Material icons → custom vector assets on Android+iOS; one confirmed miss in SubscriptionCard.kt
metadata:
  type: project
---

Icon replacement was applied broadly across auth, send button, AI goal icons, premium badge (HeroCard), meal types, workout stats, edit/delete, and muscle group drawables.

**Confirmed miss:** Android `SubscriptionCard.kt` still uses `Icons.Filled.WorkspacePremium` (both hasPro branches). The `ic_premium_badge` asset exists and is correctly used in `HeroCard.kt`. iOS `SubscriptionCardView.swift` also still uses `Image(systemName: "medal.fill")`.

**Why:** The task explicitly listed ic_premium_badge as the replacement for WorkspacePremium / medal.fill. Two separate locations (profile HeroCard vs SubscriptionCard) were treated inconsistently — HeroCard was updated, SubscriptionCard was not.

**How to apply:** When reviewing future icon tasks, cross-check all call sites for the same icon class (e.g., WorkspacePremium appears in multiple files — search before approving).
