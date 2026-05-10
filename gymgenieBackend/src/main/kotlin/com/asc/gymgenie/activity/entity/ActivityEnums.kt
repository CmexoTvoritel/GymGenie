package com.asc.gymgenie.activity.entity

enum class ActivityKind {
    BINARY,   // simple done/not-done check-in (value is always 1)
    COUNTER,  // numeric counter, the user types a value (e.g. glasses of water)
    PRESET    // user picks from a predefined list of preset values (e.g. 15/30/45/60 min)
}

enum class ActivityRing {
    MOVE, // physical activity
    MIND, // mental / cognitive habits
    LIFE  // lifestyle / wellbeing habits
}
