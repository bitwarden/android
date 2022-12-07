import Foundation

enum Base32Error: Error {
    case invalidFormat
}

final class Base32 {
    private static let BASE_32_CHARS:String = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    static func fromBase32(_ rawInput: String) throws -> [UInt8] {
        var input = rawInput.uppercased()
        var cleanedInput = "";
        for c in input {
            if (BASE_32_CHARS.firstIndex(of: c) != nil){
                cleanedInput.append(c);
            }
        }

        input = cleanedInput;
        if input.count == 0 {
            return [UInt8]()
        }

        var output = [UInt8](repeating: 0, count: input.count * 5 / 8) // new byte[input.Length * 5 / 8];
        var bitIndex = 0;
        var inputIndex = 0;
        var outputBits = 0;
        var outputIndex = 0;

        while outputIndex < output.count {
            guard let byteIndex = BASE_32_CHARS.firstIndex(of: input[input.index(input.startIndex, offsetBy:inputIndex)]) else {
                throw Base32Error.invalidFormat
            }
            
            let byteIndexInt = BASE_32_CHARS.distance(from: BASE_32_CHARS.startIndex, to: byteIndex)

            let bits = min(5 - bitIndex, 8 - outputBits);
            output[outputIndex] <<= bits;
            output[outputIndex] |= (UInt8)(byteIndexInt >> (5 - (bitIndex + bits)));

            bitIndex += bits;
            if (bitIndex >= 5)
            {
                inputIndex += 1;
                bitIndex = 0;
            }

            outputBits += bits;
            if (outputBits >= 8)
            {
                outputIndex += 1;
                outputBits = 0;
            }
        }

        return output;
    }
}
