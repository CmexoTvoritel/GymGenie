import SwiftUI

/// Shared color palette for the premium redesign.
///
/// Values derive from the project design language:
/// - Accent orange  — oklch(0.72 0.18 45)  ≈ #F07030
/// - Deep ink       — oklch(0.18 0.01 50)  ≈ #292420
/// - Warm off-white — oklch(0.98 0.005 60) ≈ #FAF9F7
/// - Soft card      — oklch(0.96 0.008 60) ≈ #F3F2EF
enum Palette {
    static let accentOrange = Color(red: 0.941, green: 0.439, blue: 0.188) // ≈ #F07030
    static let deepInk      = Color(red: 0.161, green: 0.141, blue: 0.125) // ≈ #292420
    static let warmOffWhite = Color(red: 0.980, green: 0.976, blue: 0.969) // ≈ #FAF9F7
    static let softCard     = Color(red: 0.953, green: 0.949, blue: 0.937) // ≈ #F3F2EF

    // Secondary palette (rings + activity rows) — Apple Fitness inspired.
    static let ringMovement  = Color(red: 0.957, green: 0.263, blue: 0.329) // coral red
    static let ringActivity  = Color(red: 0.298, green: 0.831, blue: 0.482) // fresh green
    static let ringWarmups   = Color(red: 0.231, green: 0.671, blue: 0.969) // sky blue

    // New activity-ring palette (home redesign): MOVE / MIND / LIFE.
    static let ringMove = Color(red: 1.0, green: 0.231, blue: 0.361)   // #FF3B5C
    static let ringMind = Color(red: 0.239, green: 0.863, blue: 0.518) // #3DDC84
    static let ringLife = Color(red: 0.231, green: 0.616, blue: 1.0)   // #3B9DFF
    static let activityCardBorder = Color(red: 0.929, green: 0.929, blue: 0.937) // #EDEDEF

    static let activityWater      = Color(red: 0.231, green: 0.671, blue: 0.969)
    static let activityWalking    = Color(red: 0.298, green: 0.831, blue: 0.482)
    static let activityStretching = Color(red: 0.984, green: 0.502, blue: 0.580)
    static let activityMeditation = Color(red: 0.639, green: 0.522, blue: 0.929)

    // Catalog pastel backgrounds.
    static let pastelCoral    = Color(red: 1.000, green: 0.910, blue: 0.878) // #FFE8E0
    static let pastelBlue     = Color(red: 0.878, green: 0.941, blue: 1.000) // #E0F0FF
    static let pastelLavender = Color(red: 0.929, green: 0.878, blue: 1.000) // #EDE0FF
    static let pastelGreen    = Color(red: 0.878, green: 0.961, blue: 0.910) // #E0F5E8
    static let pastelYellow   = Color(red: 1.000, green: 0.973, blue: 0.878) // #FFF8E0

    static let mutedText = Color(red: 0.463, green: 0.447, blue: 0.416)

    // --- Workout session palette -------------------------------------------
    // Per design spec the workout session uses coral as the primary action color.
    static let coral      = Color(red: 1.000, green: 0.353, blue: 0.235) // #FF5A3C
    static let coralLight = Color(red: 1.000, green: 0.353, blue: 0.235).opacity(0.12)
    static let inkDark    = Color(red: 0.102, green: 0.102, blue: 0.137) // #1A1A23 — exercise-image background

    // --- Neutrals & primary scale (used by the glass bottom navigation bar) -
    static let neutrals400 = Color(red: 0.718, green: 0.702, blue: 0.671) // #B7B3AB
    static let neutrals700 = Color(red: 0.361, green: 0.349, blue: 0.325) // #5C5953
    static let neutrals900 = Color(red: 0.122, green: 0.114, blue: 0.102) // #1F1D1A
    static let primary400  = accentOrange
}
