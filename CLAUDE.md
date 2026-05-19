# Project overview

This project is an AI-powered sports mobile application.

Architecture:
- Backend: Kotlin + Spring Boot
- Mobile shared logic: Kotlin Multiplatform (KMM)
- iOS UI: SwiftUI
- Android UI: Jetpack Compose
- Navigation on mobile: Decompose
- The codebase should follow Clean Architecture, SOLID, clean code principles, strong modularity, explicit interfaces, and clear separation of concerns.

# Core engineering principles

Always prefer:
- clear architecture over quick hacks
- explicit contracts over implicit coupling
- interface-driven design
- feature modularization when justified
- isolated domain logic
- careful DTO -> domain -> UI mapping
- testable code
- safe error handling
- maintainable navigation structure
- readability and long-term scalability

Never introduce:
- hidden coupling between modules
- UI logic inside domain layer
- transport models leaking into domain
- vague naming
- large god classes
- unstructured exception handling
- architecture shortcuts without explaining tradeoffs

# Mobile architecture expectations

For mobile-related work:
- Respect KMM boundaries strictly
- Shared logic belongs in shared Kotlin modules when it is truly cross-platform
- Platform-specific UI and integrations must remain platform-specific
- Use Decompose thoughtfully and keep navigation predictable, testable, and isolated from view rendering details
- Prefer unidirectional data flow where appropriate
- Keep state models explicit
- Favor composition over inheritance
- Avoid overengineering, but do not be afraid of multi-module design when it improves maintainability

# Backend architecture expectations

For backend-related work:
- Use clean layering: controller/api, application/service, domain, infrastructure
- Validate input explicitly
- Handle exceptions consistently
- Preserve transaction boundaries intentionally
- Design APIs and services with maintainability in mind
- Keep security, observability, and future extensibility in mind
- Avoid leaking persistence details into business logic

# Agent routing policy

Claude acts as the orchestrator and must delegate specialized work.

Use the `mobile-architect` agent for:
- KMM shared logic
- mobile architecture
- SwiftUI / Compose integration
- Decompose navigation
- presentation/state handling
- repositories, use cases, mappers on the mobile side
- mobile DI and modularization

Use the `backend-architect` agent for:
- Kotlin Spring backend work
- API contracts
- services, controllers, repositories
- persistence, transactions, validation
- auth/security/backend integrations
- backend modularization and architecture

Use the `code-reviewer` agent after every implementation that changes code, structure, contracts, or behavior.

# Required workflow

For every task:
1. Determine whether the task belongs to mobile, backend, or both
2. Delegate implementation to the appropriate specialized agent
3. After implementation is complete, always run `code-reviewer`
4. The reviewer must inspect:
   - the user request
   - the actual changes
   - architectural consistency
   - correctness and completeness
   - possible edge cases and missing handling
5. If reviewer feedback is valid and materially improves the result, send the task back to the corresponding implementation agent
6. The reviewer feedback loop is limited to 2 iterations maximum per user request
7. If the reviewer finds no meaningful issues, it must say so explicitly and avoid inventing unnecessary changes

# Review loop rules

The reviewer must not suggest cosmetic or low-value changes just to produce feedback.
The reviewer should focus on:
- correctness
- architecture
- missing branches in logic
- error handling
- unclear contracts
- risky assumptions
- maintainability
- consistency with the task

The reviewer may send improvement recommendations back for implementation at most 2 times.
After 2 review iterations, stop the loop and present:
- what was fixed
- what remains as acceptable tradeoff or open risk

# Change quality bar

Before finishing, verify:
- code is aligned with Clean Architecture and SOLID where appropriate
- new abstractions are justified
- names are clear
- exceptions are handled intentionally
- data models are mapped at proper boundaries
- navigation/state updates are robust
- changes are not partial if the task implied complete flow coverage
- code fits the existing project style unless that style is clearly harmful

# Communication style

Be direct, technical, and honest.
Do not invent problems if the implementation is already solid.
Do not overcomplicate the solution.
When there are tradeoffs, explain them clearly.
Prefer precise recommendations over generic advice.