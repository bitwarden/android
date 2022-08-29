//
//  ContentView.swift
//  bitwarden WatchKit Extension
//
//  Created by Federico Andrés Maccaroni on 25/08/2022.
//

import SwiftUI

struct ContentView: View {
    @StateObject fileprivate var viewModel = CiphersViewModel()
    @ObservedObject private var connectivityManager = WatchConnectivityManager.shared
    
    var body: some View {
        VStack{
            Button("Tap me!", action: {
              WatchConnectivityManager.shared.send("Hello World!\n\(Date().ISO8601Format())")
            })
            List(viewModel.ciphers){ cipher in
                Text("\(cipher.name): \(cipher.login.totp)")
                    .padding()
            }
        }
        .alert(item: $connectivityManager.notificationMessage) { message in
            Alert(title: Text(message.text),
                       dismissButton: .default(Text("Dismiss")))
        }
    }
}

private class CiphersViewModel: ObservableObject {
  @Published var ciphers: [Cipher] = Cipher.samples
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
