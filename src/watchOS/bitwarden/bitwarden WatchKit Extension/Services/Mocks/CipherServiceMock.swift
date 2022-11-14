import Foundation

class CipherServiceMock: CipherServiceProtocol{
    func getCipher(_ id: String) -> Cipher? {
        return CipherMock.ciphers.first { ci in
            ci.id == id
        }
    }
    
    func deleteAll() {
    }
    
    func saveCiphers(_ ciphers: [Cipher], completionHandler: @escaping () -> Void) {
    }
    
    private var ciphers = [Cipher]()
    
    init() {
        ciphers = CipherMock.ciphers
    }
    
    func fetchCiphers() -> [Cipher] {
        return ciphers
    }
}
