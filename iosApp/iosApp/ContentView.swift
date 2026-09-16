import UIKit
import SwiftUI
import Shared

#if DEBUG
private let developmentToolsEnabled = true
private let uiCatalogRequested = ProcessInfo.processInfo.arguments.contains("--ui-catalog")
#else
private let developmentToolsEnabled = false
private let uiCatalogRequested = false
#endif

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Self.Context) -> UIViewController {
        MainViewControllerKt.MainViewController(
            developmentToolsEnabled: developmentToolsEnabled,
            uiCatalogRequested: uiCatalogRequested
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Self.Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}
