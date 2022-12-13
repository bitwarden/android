import SwiftUI

struct CipherListView: View {
    @ObservedObject var viewModel = CipherListViewModel(CipherService.shared)
    
    let AVATAR_ID: String = "avatarId"
    @State private var contentOffset = CGFloat(0)
    @State private var initialOffset = CGFloat(0)
        
    var isHeaderVisible: Bool {
        if !viewModel.searchTerm.isEmpty {
            return true
        }
        
        let threshold = initialOffset + 15
        return viewModel.filteredCiphers.count > 1 && contentOffset > threshold
    }
    
    var body: some View {
        NavigationView {
            GeometryReader { geometry in
                ScrollViewReader { scrollProxy in
                    TrackableWithHeaderListView { offset in
                        withAnimation {
                            contentOffset = offset?.y ?? 0
                        }
                    }  headerContent: {
                        Section() {
                            ZStack {
                                searchContent
                                    .padding(5)
                                    .background(
                                        RoundedRectangle(cornerRadius: 5)
                                            .foregroundColor(Color.ui.primary)
                                            .frame(width: geometry.size.width,
                                                   alignment: .leading)
                                    )
                                    .opacity(isHeaderVisible ? 1 : 0)
                            }
                            .background(
                                RoundedRectangle(cornerRadius: 5)
                                .foregroundColor(Color.black)
                                .frame(width: geometry.size.width, height: 60)
                            )
                            .offset(y:isHeaderVisible ? 0 : 5)
                            .padding(0)
                        }
                    } content: {
                        if viewModel.user?.email != nil {
                            Section() {
                                avatarHeader
                                    .id(AVATAR_ID)
                                    .background(
                                        RoundedRectangle(cornerRadius: 5)
                                            .foregroundColor(Color.ui.avatarItemBackground)
                                            .frame(width: geometry.size.width + 10, height: 50)
                                    )
                                    .padding(0)
                            }
                        }
                        ForEach(viewModel.filteredCiphers, id: \.id) { cipher in
                            NavigationLink(destination: CipherDetailsView(cipher: cipher)){
                                CipherItemView(cipher, geometry.size.width)
                            }
                            .listRowInsets(EdgeInsets())
                            .listRowBackground(Color.clear)
                            .padding(0)
                        }
                    }
                    .emptyState(viewModel.filteredCiphers.isEmpty, emptyContent: {
                        emptyContent
                            .frame(width: geometry.size.width, alignment: .center)
                    })
                    .onReceive(self.viewModel.$updateHack) { _ in
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                            scrollProxy.scrollTo(AVATAR_ID, anchor: .top)
                            
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.15){
                                self.initialOffset = self.contentOffset
                            }
                        }
                    }
                }
            }
        }
        .navigationTitle("Bitwarden")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
#if targetEnvironment(simulator) // for the preview
            self.viewModel.fetchCiphers()
#else
            self.viewModel.checkStateAndFetch()
#endif
        }
        .fullScreenCover(isPresented: $viewModel.showingSheet) {
            BWStateView(viewModel.currentState)
        }
    }
    
    var searchContent: some View {
        HStack {
            Image(systemName: "magnifyingglass")
                .foregroundColor(Color(.white))
                .frame(width: 20, height: 30)
            TextField("", text: $viewModel.searchTerm)
                .foregroundColor(.white)
                .frame(width: .infinity, height: 33)
                .placeholder(when: viewModel.searchTerm.isEmpty) {
                    Text("Search").foregroundColor(.white)
                }
        }
    }
    
    var avatarHeader: some View {
        HStack {
            AvatarView(viewModel.user)
            Text(viewModel.user!.email!)
                .font(.headline)
                .lineLimit(1)
                .truncationMode(.tail)
        }
    }
    
    var emptyContent: some View {
        VStack(alignment: .center) {
            Image("EmptyListPlaceholder")
                .resizable()
                .scaledToFit()
                .padding(20)
            Text("ThereAreNoItemsToList")
                .foregroundColor(Color.white)
                .font(.headline)
                .multilineTextAlignment(.center)
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        var v = CipherListView()
        StateService.shared.currentState = .valid
        v.viewModel = CipherListViewModel(CipherServiceMock())
        v.viewModel.user = User(id: "zxc", email: "testing@test.com", name: "Tester")
        return v
    }
}
