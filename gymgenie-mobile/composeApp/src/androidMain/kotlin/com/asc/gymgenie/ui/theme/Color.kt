package com.asc.gymgenie.ui.theme

import androidx.compose.ui.graphics.Color

val Primary = Color(0xFF2CC1E3)
val PrimaryDark = Color(0xFF1A9ABB)
val Background = Color(0xFFF5F7FA)
val Surface = Color.White
val OnPrimary = Color.White
val OnBackground = Color(0xFF1A1A2E)
val OnSurface = Color(0xFF1A1A2E)
val OnSurfaceVariant = Color(0xFF6B7280)
val Error = Color(0xFFE53935)
val IllustrationBackground = Color(0xFFE0F4FA)
val Green = Color(0xFF4CAF50)
val AccentGreen = Color(0xFF2ECC71)

// Workout session palette (per design spec).
// Coral is the primary action color for the in-session screens.
val Coral = Color(0xFFFF5A3C)
val CoralLight = Color(0x1FFF5A3C)
val CoralDark = Color(0xFF1A1A2E)

// --- Premium redesign palette -----------------------------------------------
// Derived from the design tokens: orange accent, deep ink, warm off-white,
// soft card. Intentionally kept as independent constants so legacy screens
// that still lean on Primary/Background are not disrupted.
val AccentOrange = Color(0xFFF07030)      // oklch(0.72 0.18 45)
val DeepInk = Color(0xFF292420)           // oklch(0.18 0.01 50)
val WarmOffWhite = Color(0xFFFAF9F7)      // oklch(0.98 0.005 60)
val SoftCard = Color(0xFFF3F2EF)          // oklch(0.96 0.008 60)
val MutedText = Color(0xFF76726A)
val PillBg = SoftCard

// Ring & activity secondary palette.
val RingMovement = Color(0xFFF43F55)
val RingActivity = Color(0xFF4DD47B)
val RingWarmups = Color(0xFF3BACF7)

// New activity-ring palette aligned with the home redesign spec.
// MOVE = movement-style activities, MIND = focus/calm, LIFE = lifestyle/habits.
val RingMove = Color(0xFFFF3B5C)
val RingMind = Color(0xFF3DDC84)
val RingLife = Color(0xFF3B9DFF)
val ActivityCardBorder = Color(0xFFEDEDEF)

val ActivityWater = Color(0xFF3BACF7)
val ActivityWalking = Color(0xFF4DD47B)
val ActivityStretching = Color(0xFFFB8094)
val ActivityMeditation = Color(0xFFA385ED)

// Catalog pastel backgrounds.
val PastelCoral = Color(0xFFFFE8E0)
val PastelBlue = Color(0xFFE0F0FF)
val PastelLavender = Color(0xFFEDE0FF)
val PastelGreen = Color(0xFFE0F5E8)
val PastelYellow = Color(0xFFFFF8E0)

// --- Neutrals & primary scale (used by the glass bottom navigation bar) -----
// Mapped from the Flutter reference design tokens.
val Neutrals400 = Color(0xFFB7B3AB)
val Neutrals700 = Color(0xFF5C5953)
val Neutrals900 = Color(0xFF1F1D1A)
val Primary400 = AccentOrange
