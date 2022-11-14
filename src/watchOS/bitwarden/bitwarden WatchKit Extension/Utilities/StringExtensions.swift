import Foundation

extension String {
    static func isEmpty(_ s:String?) -> Bool {
        guard let s = s else {
            return true
        }
        
        return s.isEmpty
    }
    
    static func isEmptyOrWhitespace(_ s: String?) -> Bool {
        guard let s = s else {
            return true
        }
        
        return s.trimmingCharacters(in: .whitespaces).isEmpty
    }
}
