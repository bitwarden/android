import Foundation
import CoreData

class CipherListViewModel : ObservableObject {
    @Published var ciphers: [Cipher] = []
    
    init(){
        fetchCiphers()
    }
    
    func fetchCiphers() {
        let result: Result<[CipherEntity], Error> = CoreDataHelper.shared.fetch(CipherEntity.self)
        switch result {
        case .success(let success):
            ciphers = success.map { entity in entity.toCipher() }
        case .failure(let failure):
            print(failure)
        }
    }
}
