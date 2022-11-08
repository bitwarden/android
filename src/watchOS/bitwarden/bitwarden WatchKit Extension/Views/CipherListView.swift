import SwiftUI

struct CipherListView: View {
    @ObservedObject var viewModel = CipherListViewModel(CipherService.shared)
    //@ObservedObject private var connectivityManager = //WatchConnectivityManager.shared
    
    @State var stat: String = "No Ciphers"
    
    var body: some View {
        NavigationView {
            VStack{
                HStack{
                    Button("D") {
                        self.stat = "deleting"
                        self.viewModel.deleteAll()
                    }
                    Button("R") {
                        self.stat = "refreshing"
                        self.viewModel.refreshCiphers()
                    }
                }
                List(viewModel.ciphers){ cipher in
                    Text(cipher.name ?? "")
                        .padding()
                }
                .emptyState(viewModel.ciphers.isEmpty, emptyContent: {
                    // TODO
                    Text(stat)
                        .foregroundColor(Color.white)
                        .font(.title3)
                        .onTapGesture {
                            self.stat = "loading"
                            self.viewModel.refreshCiphers()
                        }
                })
            }
        }
        .onAppear {
            self.viewModel.fetchCiphers()
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
