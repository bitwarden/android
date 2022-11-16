import Foundation
import CoreData

class CipherListViewModel : ObservableObject {
    var cipherService: CipherServiceProtocol
    
    @Published var ciphers: [Cipher] = []
    @Published var showingSheet = false
    @Published var currentState: BWState
    
    init(_ cipherService: CipherServiceProtocol){
        self.cipherService = cipherService
        self.currentState = StateService.shared.currentState
        self.showingSheet = currentState != .valid
    }
    
    func fetchCiphers() {
        if currentState == .valid {
            ciphers = CipherMock.ciphers
        }

        //ciphers = cipherService.fetchCiphers()
    }
}
