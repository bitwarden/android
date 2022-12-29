import Foundation
import CoreData

protocol CipherServiceProtocol {
    func getCipher(_ id: String) -> Cipher?
    func fetchCiphers(_ withUserId: String?) -> [Cipher]
    func saveCiphers(_ ciphers: [Cipher], completionHandler: @escaping () -> Void)
    func deleteAll(_ withUserId: String?, completionHandler: @escaping () -> Void)
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
    func fetchCiphers(_ withUserId: String?) -> [Cipher] {
        let result: Result<[CipherEntity], Error> = dbHelper.fetch(CipherEntity.self, "CipherEntity", predicate: withUserId == nil ? nil : NSPredicate(format: "userId = %@", withUserId!))
        switch result {
        case .success(let success):
            return success.map { entity in entity.toCipher() }
        case .failure(let error):
            fatalError(error.localizedDescription)
        }
    }
    
    func saveCiphers(_ ciphers: [Cipher], completionHandler: @escaping () -> Void) {
        let cipherIds = ciphers.map { $0.id }
        deleteAll(ciphers[0].userId, notIn: cipherIds) {
            self.dbHelper.insertBatch("CipherEntity", items: ciphers) { item, context in
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
    }
    
    func deleteAll(_ withUserId: String? = nil, completionHandler: @escaping () -> Void) {
        let predicate = withUserId == nil ? nil : NSPredicate(format: "userId = %@", withUserId!)
        dbHelper.deleteAll("CipherEntity", predicate: predicate, completionHandler: completionHandler)
    }
    
    func deleteAll(_ withUserId: String? = nil, notIn: [String], completionHandler: @escaping () -> Void) {
        var predicateList : [NSPredicate] = []
        if let userId = withUserId {
            predicateList.append(NSPredicate(format: "userId = %@", userId))
        }
        predicateList.append(NSPredicate(format: "NOT (id in %@)", notIn))
        let predicate = NSCompoundPredicate(type: .and, subpredicates: predicateList)
        dbHelper.deleteAll("CipherEntity", predicate: predicate, completionHandler: completionHandler)
    }
}
