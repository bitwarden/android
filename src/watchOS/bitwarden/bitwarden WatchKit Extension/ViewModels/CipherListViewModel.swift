import Foundation
import CoreData

class CipherListViewModel : ObservableObject {
    var cipherService: CipherServiceProtocol
    
    @Published var ciphers: [Cipher] = []
    
    init(_ cipherService: CipherServiceProtocol){
        self.cipherService = cipherService
        fetchCiphers()
    }
    
    func fetchCiphers() {
        ciphers = cipherService.fetchCiphers()
    }
    
    func refreshCiphers(){
        var ciph = [
            Cipher(id: "1", name: "GGG", organizationUseTotp: true, login: Login(username: "user1", totp: "555 444")),
            Cipher(id: "2", name: "asdasd", organizationUseTotp: true, login: Login(username: "user2", totp: "567 435")),
            Cipher(id: "3", name: "asdfas", organizationUseTotp: true, login: Login(username: "user3", totp: "123 456")),
            Cipher(id: "4", name: "mate", organizationUseTotp: true, login: Login(username: "user2", totp: "111 222")),
            Cipher(id: "5", name: "yerba", organizationUseTotp: true, login: Login(username: "user5", totp: "333 555")),
            Cipher(id: "6", name: "Wole", organizationUseTotp: true, login: Login(username: "user7", totp: "567 239")),
        ]
        
        cipherService.saveCiphers(ciph){
            self.fetchCiphers()
        }
    }
    
    func deleteAll(){
        cipherService.deleteAll()
    }
}
