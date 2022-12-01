import SwiftUI

struct AvatarView: View {
    var circleColor = Color.white
    var textColor = Color.black
    var initials = ""
    
    init(_ user: User?) {
        let source = user?.name ?? user?.email
        var upperCaseText: String? = nil
        
        if source == nil || source!.isEmpty {
            initials = ".."
        } else if source!.count > 1 {
            upperCaseText = source!.uppercased()
            initials = getFirstLetters(upperCaseText!, 2)
        } else {
            upperCaseText = source!.uppercased()
            initials = upperCaseText!
        }
        
        circleColor = stringToColor(str: user?.id ?? upperCaseText, fallbackColor: Color(hex: "#FFFFFF33")!)
        textColor = textColorFromBgColor(circleColor)
    }
    
    var body: some View {
        ZStack {
            Circle()
                .foregroundColor(circleColor)
                .frame(width: 30, height: 30)
            Text(initials)
                .font(.footnote)
                .foregroundColor(textColor)
        }
    }
    
    func stringToColor(str: String?, fallbackColor: Color) -> Color {
        guard let str = str else {
            return fallbackColor
        }
        
        var hash = 0
        for char in str {
            let uniSca = String(char).unicodeScalars
            let intCharValue = Int(uniSca[uniSca.startIndex].value)

            hash = intCharValue + ((hash << 5) &- hash)
        }
        var color = "#"
        for i in 0..<3 {
            let value = (hash >> (i * 8)) & 0xff
            color += String(value, radix: 16).leftPadding(toLength: 2, withPad: "0")
        }
        return Color(hex: color) ?? fallbackColor
    }
    
    func textColorFromBgColor(_ bgColor: Color, threshold: CGFloat = 0.65) -> Color {
        let (r, g, b, _) = bgColor.components
        let luminance = r * 0.299 + g * 0.587 + b * 0.114;
        return luminance > threshold ? Color.black : Color.white;
    }

    func getFirstLetters(_ data: String, _ charCount: Int) -> String {
        let sanitizedData = data.trimmingCharacters(in: CharacterSet.whitespaces)
        let parts = sanitizedData.split(separator: " ")
        
        if parts.count > 1 && charCount <= 2 {
            var text = "";
            for i in 0..<charCount {
                text += parts[i].prefix(1);
            }
            return text;
        }
        if sanitizedData.count > 2 {
            return String(sanitizedData.prefix(2))
        }
        return sanitizedData;
    }
}

struct AvatarView_Previews: PreviewProvider {
    static var previews: some View {
        AvatarView(User(id: "zxc", email: "asdfasdf@gmail.com", name: "John Snow"))
    }
}
