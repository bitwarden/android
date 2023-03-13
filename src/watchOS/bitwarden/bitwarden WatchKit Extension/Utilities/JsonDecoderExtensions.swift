import Foundation

extension JSONDecoder.KeyDecodingStrategy {
    static var upperToLowerCamelCase: JSONDecoder.KeyDecodingStrategy {
        return .custom { codingKeys in

            var key = JSONAnyCodingKey(codingKeys.last!)

            if let firstChar = key.stringValue.first {
                key.stringValue.replaceSubrange(
                    ...key.stringValue.startIndex, with: String(firstChar).lowercased()
                )
            }
            return key
        }
    }
}

struct JSONAnyCodingKey : CodingKey {
    var stringValue: String
    var intValue: Int?

    init(_ base: CodingKey) {
        self.init(stringValue: base.stringValue, intValue: base.intValue)
    }
    
    init(stringValue: String) {
        self.stringValue = stringValue
    }

    init(intValue: Int) {
        self.stringValue = "\(intValue)"
        self.intValue = intValue
    }

    init(stringValue: String, intValue: Int?) {
        self.stringValue = stringValue
        self.intValue = intValue
    }
}
