import Foundation

final class KeychainHelper {
    
    static let standard = KeychainHelper()
    let genericService = "com.8bit.bitwarden.watch.kc"
    
    private init() {}
    
    func hasDeviceOwnerAuth() -> Bool {
        let query: [String:Any] = [
            kSecClass as String : kSecClassGenericPassword,
            kSecAttrAccount as String : UUID().uuidString,
            kSecAttrAccessible as String: kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly,
            kSecValueData as String: "hasPass".data(using: String.Encoding.utf8)!,
            kSecReturnAttributes as String : kCFBooleanTrue as Any
        ]
        
        var dataTypeRef: AnyObject?
        var status = withUnsafeMutablePointer(to: &dataTypeRef) {
            SecItemCopyMatching(query as CFDictionary, UnsafeMutablePointer($0))
        }
        
        if status == errSecItemNotFound {
            let createStatus = SecItemAdd(query as CFDictionary, nil)
            guard createStatus == errSecSuccess else {
                return false
            }
            status = withUnsafeMutablePointer(to: &dataTypeRef) {
                SecItemCopyMatching(query as CFDictionary, UnsafeMutablePointer($0))
            }
        }
        
        guard status == errSecSuccess else {
            return false
        }
        
        return true
    }
    
    func read<T>(_ key: String, _ type: T.Type) -> T? where T : Codable {
        guard let data = read(key) else {
            return nil
        }
        
        do {
            let item = try JSONDecoder().decode(type, from: data)
            return item
        } catch {
            Log.e("Fail to decode item for keychain: \(error)")
            return nil
        }
    }
        
    func save<T>(_ item: T, key: String) where T : Codable {
        
        do {
            let data = try JSONEncoder().encode(item)
            save(data, key)
            
        } catch {
            Log.e("Fail to encode item for keychain: \(error)")
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
        if let _ = read(key) {
            delete(key)
        }
        
        let query = [
            kSecValueData: data,
            kSecClass: kSecClassGenericPassword,
            kSecAttrService: genericService,
            kSecAttrAccount: key,
            kSecAttrAccessible: kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly
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
            Log.e("Error: \(status)")
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
