import SwiftUI

struct ContentView: View {
    @ObservedObject private var connectivityManager = WatchConnectivityManager.shared
    
    var body: some View {
        VStack{
            Button("Main Tap me!", action: {
              WatchConnectivityManager.shared.send("From main app")
            })
//            List(viewModel.ciphers){ cipher in
//                Text("\(cipher.name): \(cipher.login.totp)")
//                    .padding()
//            }
        }
        .alert(item: $connectivityManager.notificationMessage) { message in
            Alert(title: Text(message.text),
                       dismissButton: .default(Text("Dismiss")))
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
