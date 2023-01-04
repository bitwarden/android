import Foundation
import SwiftUI

/// List that has offset tracking and a header
///
/// - Note: Based on:  https://stackoverflow.com/questions/74047146/tracking-scroll-position-in-a-list-swiftui
///
struct TrackableWithHeaderListView<HeaderContent:View, Content: View>: View {
    let offsetChanged: (CGPoint?) -> Void
    let headerContent: HeaderContent
    let content: Content
    
    init(offsetChanged: @escaping (CGPoint?) -> Void = { _ in }, @ViewBuilder headerContent: () -> HeaderContent, @ViewBuilder content: () -> Content) {
        self.offsetChanged = offsetChanged
        self.headerContent = headerContent()
        self.content = content()
    }
    
    var body: some View {
        List {
            GeometryReader { geometry in
                headerContent
                    .preference(key: ScrollOffsetPreferenceKey.self, value: geometry.frame(in: .named("ListView")).origin)
            }
            .frame(width: .infinity)
            content
        }
        .coordinateSpace(name: "ListView")
        .onPreferenceChange(ScrollOffsetPreferenceKey.self, perform: offsetChanged)
    }
}

private struct ScrollOffsetPreferenceKey: PreferenceKey {
    static var defaultValue: CGPoint? = nil
    static func reduce(value: inout CGPoint?, nextValue: () -> CGPoint?) {
        if let nextValue = nextValue() {
            value = nextValue
        }
    }
}
