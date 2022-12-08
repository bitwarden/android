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
    
    func leftPadding(toLength: Int, withPad character: Character) -> String {
        let currentLength = self.count
        if currentLength < toLength {
            return String(repeatElement(character, count: toLength - currentLength)) + self
        } else {
            return String(self.suffix(toLength))
        }
    }
}
