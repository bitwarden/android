import Foundation

class CipherServiceMock: CipherServiceProtocol{
    func getCipher(_ id: String) -> Cipher? {
        return Cipher(id: "1", name: "Site 1", organizationUseTotp: true, login: Login(username: "user1", totp: "555 444"))
    }
    
    func deleteAll() {
    }
    
    func saveCiphers(_ ciphers: [Cipher], completionHandler: @escaping () -> Void) {
    }
    
    private var ciphers = [Cipher]()
    
    init() {
        ciphers = [
            Cipher(id: "1", name: "Site 1", organizationUseTotp: true, login: Login(username: "user1", totp: "555 444")),
            Cipher(id: "2", name: "Site 2", organizationUseTotp: true, login: Login(username: "user2", totp: "567 435")),
            Cipher(id: "3", name: "Really long name for a site that is used for a totp", organizationUseTotp: true, login: Login(username: "user3", totp: "123 456"))
        ]
    }
    
    func fetchCiphers() -> [Cipher] {
        return ciphers
    }
}
