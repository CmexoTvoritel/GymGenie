---
name: Keyboard dismissal lives in shared UI components
description: GymGenieButton (iOS+Android) and AiFlowScreen private components (ChipGroup, FlowHeader, PrimaryButton) already dismiss the keyboard on click; do not re-add at each call site.
type: project
---

`GymGenieButton` (iOS Components/GymGenieButton.swift, Android ui/components/GymGenieButton.kt) clears focus / resigns first responder inside its own click handler. The same applies to the private composables/structs in `AiFlowScreen.kt` and `AiCoachView.swift` that are reused across the AI flow steps: `ChipGroup`, `FlowHeader`, `PrimaryButton`.

**Why:** when the user requested "tap on anything that is not an input dismisses the keyboard", we deliberately pushed the dismissal into shared components rather than wrapping every call site. The pattern was confirmed in the implementation thread.

**How to apply:**
- When adding a new screen that uses these shared controls, you do NOT need to add focus-dismissal logic to their actions — it is already inside.
- For NEW interactive controls on screens that contain text input (custom buttons, chips, links, header icons), do dismiss focus from inside the new control's click handler.
- For non-interactive backgrounds / scroll content of a screen with input, attach: SwiftUI `.contentShape(Rectangle()).onTapGesture { /* clear focus */ }` on the inner content container, or Compose `.pointerInput(Unit) { detectTapGestures { focusManager.clearFocus(); keyboardController?.hide() } }` on the root container or the scrolling list.
- Prefer `@FocusState` (iOS) and `LocalFocusManager` + `LocalSoftwareKeyboardController` (Android) over global `UIResponder.resignFirstResponder` when a focus-state binding is already available — keeps focus state and visual highlight in sync.
