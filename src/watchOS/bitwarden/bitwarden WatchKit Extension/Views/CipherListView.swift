import SwiftUI

struct CipherListView: View {
    @ObservedObject var viewModel = CipherListViewModel(CipherService.shared)
    @ObservedObject var watchCM = WatchConnectivityManager.shared
    
    var body: some View {
        NavigationView {
                VStack{
                    GeometryReader { geometry in
                        List {
                            ForEach(viewModel.ciphers, id: \.id) { cipher in
                                NavigationLink(destination: CipherDetailsView(cipher: cipher)){
                                    VStack(alignment: .leading){
                                        Text(cipher.name ?? "")
                                            .font(.title3)
                                            .fontWeight(.bold)
                                            .lineLimit(1)
                                            .truncationMode(.tail)
                                            .frame(maxWidth: .infinity, alignment: .leading)
                                        
                                        if cipher.login.username != nil {
                                            Text(cipher.login.username! )
                                                .font(.body)
                                                .lineLimit(1)
                                                .truncationMode(.tail)
                                                .foregroundColor(Color.ui.darkTextMuted)
                                                .frame(maxWidth: .infinity, alignment: .leading)
                                        }
                                    }
                                    .padding()
                                    .background(
                                        RoundedRectangle(cornerRadius: 5)
                                            .foregroundColor(Color.ui.itemBackground)
                                            .frame(width: geometry.size.width,
                                                   alignment: .leading)
                                    )
                                    .frame(width: geometry.size.width,
                                           alignment: .leading)
                                }
                                .listRowInsets(EdgeInsets())
                                .listRowBackground(Color.clear)
                                .padding(3)
                            }
                        }
                        .emptyState(viewModel.ciphers.isEmpty, emptyContent: {
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
                            .frame(width: geometry.size.width, alignment: .center)
                        })
                    }
                     
                }
        }
        .onAppear {
            self.viewModel.checkStateAndFetch()
        }
        .fullScreenCover(isPresented: $viewModel.showingSheet) {
            BWStateView(viewModel.currentState)
        }
        /*.alert(item: $connectivityManager.notificationMessage) { message in
            Alert(title: Text(message.text),
                       dismissButton: .default(Text("Dismiss")))
        }*/
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        var v = CipherListView()
        v.viewModel = CipherListViewModel(CipherServiceMock())
        return v
    }
}
