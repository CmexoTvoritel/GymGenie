---
name: KMP top-level functions starting with "init" get a "do" prefix in Swift
description: Kotlin/Native renames top-level `init*` functions to `doInit*` when exposed to Swift to avoid clashing with ObjC initializers
type: feedback
---

When exposing a top-level Kotlin function whose name starts with `init` (e.g. `fun initKoin()`) through a KMP framework, Kotlin/Native exports it to Swift as `doInitKoin()` (i.e. `KoinInitializerKt.doInitKoin()`), not `initKoin()`. Member functions on Kotlin objects/classes are NOT affected — only top-level functions.

**Why:** Objective-C reserves the `init` prefix for initializers, so the Kotlin/Native compiler renames Kotlin top-level `init*` functions to avoid an ABI clash. This bites every fresh KMP integration that adds an `initSomething()` bootstrap.

**How to apply:** When generating Swift call sites for KMP top-level bootstrap functions, prefer either (a) renaming the Kotlin function to something that does not start with `init` (e.g. `bootstrapKoin`, `setupKoin`), or (b) calling `do<Name>` on the Swift side. If unsure of the exact KMP version's behaviour, mention the gotcha in the change report so the iOS engineer can adjust quickly during first build.
