import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        // Bootstraps Koin once per process before any view is constructed so
        // wrappers (e.g. `UserProfileStoreWrapper`) can resolve singletons in
        // their `init`.
        KoinInitializerKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .preferredColorScheme(.light)
        }
    }
}
