---
name: project-jackson-version
description: GymGenie backend uses Jackson 3.x (tools.jackson.*) not the legacy com.fasterxml.jackson namespace
metadata:
  type: project
---

The GymGenie Spring Boot backend ships with Jackson 3.x via Spring Boot 4. Imports are `tools.jackson.databind.ObjectMapper`, `tools.jackson.core.type.TypeReference`, `tools.jackson.core.JacksonException`, and the Kotlin module is `tools.jackson.module:jackson-module-kotlin`. The legacy `com.fasterxml.jackson.*` namespace is NOT what this codebase uses for service-layer JSON.

**Why:** Spring Boot 4 + Jackson 3 introduced a new root package (`tools.jackson`). Mixing it with `com.fasterxml.jackson` types (still pulled in transitively via `jackson-databind`) would create two parallel `ObjectMapper` hierarchies and Spring would only auto-wire the Jackson 3 one.

**How to apply:** When injecting an `ObjectMapper` in a service or writing custom serializers, always import from `tools.jackson.*`. The auto-configured bean is the Jackson 3 mapper. For `readValue` with a generic type, use `tools.jackson.core.type.TypeReference` and catch `tools.jackson.core.JacksonException` for graceful degradation on malformed stored JSON.
