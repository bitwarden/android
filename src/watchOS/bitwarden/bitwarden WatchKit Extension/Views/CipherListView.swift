import SwiftUI

struct CipherListView: View {
    @ObservedObject var viewModel = CipherListViewModel(CipherService.shared)
    
    let AVATAR_ID: String = "avatarId"
        
    var isHeaderVisible: Bool {
        return !viewModel.searchTerm.isEmpty || viewModel.filteredCiphers.count > 1
    }
    
    var body: some View {
        NavigationView {
            GeometryReader { geometry in
                ScrollViewReader { scrollProxy in
                    List {
                        if isHeaderVisible {
                            Section() {
                                getSearchSection(geometry.size.width)
                            }
                        }
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
            BWStateView(viewModel.currentState, viewModel.debugText)
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
                .privacySensitive()
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
    
    func getSearchSection(_ maxWidth:CGFloat) -> some View {
        return ZStack {
            searchContent
                .padding(5)
                .background(
                    RoundedRectangle(cornerRadius: 5)
                        .foregroundColor(Color.ui.primary)
                        .frame(width: maxWidth,
                               alignment: .leading)
                )
        }
        .background(
            RoundedRectangle(cornerRadius: 5)
                .foregroundColor(Color.black)
                .frame(width: maxWidth, height: 60)
        )
        .padding(0)
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
