import Combine
import CoreData
import Foundation

class CipherListViewModel : ObservableObject {
    var cipherService: CipherServiceProtocol
    var watchConnectivityManager = WatchConnectivityManager.shared
    
    @Published var ciphers: [Cipher] = []
    @Published var showingSheet = false
    @Published var currentState = BWState.valid
    @Published var user: User?
    
    private var subscriber: AnyCancellable?
    
    init(_ cipherService: CipherServiceProtocol){
        self.cipherService = cipherService
                
        subscriber = watchConnectivityManager.watchConnectivitySubject.sink { completion in
                print("WCM subject: \(completion)")
        } receiveValue: { value in
            self.checkStateAndFetch(value.state)
        }
    }
    
    func checkStateAndFetch(_ state: BWState? = nil){
        currentState = state ?? StateService.shared.currentState
        showingSheet = currentState != .valid
        
        if state != nil {
            return
        }
        
        user = StateService.shared.getUser()
        
        guard currentState == .valid else {
            ciphers = []
            return
        }
        
        self.fetchCiphers()
    }
    
    func fetchCiphers() {
        let c = cipherService.fetchCiphers(user?.id)
        DispatchQueue.main.async {
            self.ciphers = c
        }
    }
}
