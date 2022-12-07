import Foundation

class CipherServiceMock: CipherServiceProtocol{
    func fetchCiphers(_ withUserId: String?) -> [Cipher] {
        return ciphers
    }
    
    func deleteAll(_ withUserId: String?, completionHandler: @escaping () -> Void) {
        completionHandler()
    }
    
    func getCipher(_ id: String) -> Cipher? {
        return CipherMock.ciphers.first { ci in
            ci.id == id
        }
    }
    
    func saveCiphers(_ ciphers: [Cipher], completionHandler: @escaping () -> Void) {
    }
    
    private var ciphers = [Cipher]()
    
    init() {
        ciphers = CipherMock.ciphers
    }
}
