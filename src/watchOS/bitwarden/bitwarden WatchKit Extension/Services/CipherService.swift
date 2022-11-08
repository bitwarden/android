import Foundation
import CoreData

protocol CipherServiceProtocol {
    func getCipher(_ id: String) -> Cipher?
    func fetchCiphers() -> [Cipher]
    func saveCiphers(_ ciphers: [Cipher], completionHandler: @escaping () -> Void)
    func deleteAll()
}

class CipherService {
    static let shared: CipherServiceProtocol = CipherService()
    
    var dbHelper: CoreDataHelper = CoreDataHelper.shared
    
    private init() { }
    
    func getCipher(_ id: String) -> Cipher? {
        let predicate = NSPredicate(
            format: "id = %@",
            id as CVarArg)
        let result = dbHelper.fetchFirst(CipherEntity.self, predicate: predicate)
        switch result {
        case .success(let cipherEntity):
            return cipherEntity?.toCipher()
        case .failure(_):
            return nil
        }
    }
}

// MARK: - CipherServiceProtocol
extension CipherService: CipherServiceProtocol {
    func fetchCiphers() -> [Cipher] {
        let result: Result<[CipherEntity], Error> = dbHelper.fetch(CipherEntity.self, "CipherEntity")
        switch result {
        case .success(let success):
            return success.map { entity in entity.toCipher() }
        case .failure(let error):
            fatalError(error.localizedDescription)
        }
    }
    
    func saveCiphers(_ ciphers: [Cipher], completionHandler: @escaping () -> Void){
        dbHelper.insertBatch("CipherEntity", items: ciphers) { item, context in
            guard let cipher = item as! Cipher? else { return [:] }
            let c = cipher.toCipherEntity(moContext: context)
            guard let data = try? JSONEncoder().encode(c) else
            {
                Log.e("Error converting to data")
                return [:]
            }
            
            guard let cipherDict = try? JSONSerialization.jsonObject(with: data, options: []) as? [String : Any ] else
            {
                Log.e("Error converting json data to dict")
                return [:]
            }
            return cipherDict

        } completionHandler: {
            completionHandler()
        }
    }
    
    func deleteAll(){
        dbHelper.deleteAll("CipherEntity")
    }
}
