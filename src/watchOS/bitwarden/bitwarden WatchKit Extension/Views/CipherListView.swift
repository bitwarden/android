//
//  ContentView.swift
//  bitwarden WatchKit Extension
//
//  Created by Federico Andrés Maccaroni on 25/08/2022.
//

import SwiftUI

struct CipherListView: View {
    @StateObject var viewModel = CipherListViewModel()
    @ObservedObject private var connectivityManager = WatchConnectivityManager.shared
    
    var body: some View {
        VStack{
            List(viewModel.ciphers){ cipher in
                Text(cipher.id)
                    .padding()
            }
        }
        .alert(item: $connectivityManager.notificationMessage) { message in
            Alert(title: Text(message.text),
                       dismissButton: .default(Text("Dismiss")))
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        CipherListView()
    }
}
