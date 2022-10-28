import Foundation

final class KeychainHelper {
    
    static let standard = KeychainHelper()
    let genericService = "com.8bit.bitwarden.watch.kc"
    
    private init() {}
    
    func read<T>(_ key: String, _ type: T.Type) -> T? where T : Codable {
        guard let data = read(key) else {
            return nil
        }
        
        do {
            let item = try JSONDecoder().decode(type, from: data)
            return item
        } catch {
            assertionFailure("Fail to decode item for keychain: \(error)")
            return nil
        }
    }
        
    func save<T>(_ item: T, key: String) where T : Codable {
        
        do {
            let data = try JSONEncoder().encode(item)
            save(data, key)
            
        } catch {
            assertionFailure("Fail to encode item for keychain: \(error)")
        }
    }

    // MARK: NON-GENERIC FUNC
    func read(_ key: String) -> Data? {
        let query = [
            kSecAttrService: genericService,
            kSecAttrAccount: key,
            kSecClass: kSecClassGenericPassword,
            kSecReturnData: true
        ] as CFDictionary
        
        var result: AnyObject?
        SecItemCopyMatching(query, &result)
        
        return (result as? Data)
    }

    func save(_ data: Data, _ key: String) {
        let query = [
            kSecValueData: data,
            kSecClass: kSecClassGenericPassword,
            kSecAttrService: genericService,
            kSecAttrAccount: key,
        ] as CFDictionary
        
        let status = SecItemAdd(query, nil)
        
        if status == errSecDuplicateItem {
            // Item already exist, thus update it.
            let query = [
                kSecAttrService: genericService,
                kSecAttrAccount: key,
                kSecClass: kSecClassGenericPassword,
            ] as CFDictionary

            let attributesToUpdate = [kSecValueData: data] as CFDictionary

            SecItemUpdate(query, attributesToUpdate)
        }

        
        if status != errSecSuccess {
            // Print out the error
            print("Error: \(status)")
        }
    }

    func delete(_ key: String) {
        
        let query = [
            kSecAttrService: genericService,
            kSecAttrAccount: key,
            kSecClass: kSecClassGenericPassword,
            ] as CFDictionary
        
        SecItemDelete(query)
    }
}
