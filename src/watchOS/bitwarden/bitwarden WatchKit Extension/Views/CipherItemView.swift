import SwiftUI

struct CipherItemView: View {
    let cipher:Cipher
    let maxWidth:CGFloat
    
    init(_ cipher:Cipher, _ maxWidth:CGFloat) {
        self.cipher = cipher
        self.maxWidth = maxWidth
    }
    
    var body: some View {
        VStack(alignment: .leading) {
            if cipher.id == "-1" {
                // Workaround: To display 0 results on search
                // and the message to be localized
                Text(LocalizedStringKey(cipher.name!))
                    .font(.title3)
                    .fontWeight(.bold)
                    .lineLimit(1)
                    .truncationMode(.tail)
                    .frame(maxWidth: .infinity, alignment: .leading)
            } else {
                Text(cipher.name ?? "")
                    .font(.title3)
                    .fontWeight(.bold)
                    .lineLimit(1)
                    .truncationMode(.tail)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            
            if cipher.login.username != nil {
                Text(cipher.login.username! )
                    .font(.body)
                    .lineLimit(1)
                    .truncationMode(.tail)
                    .foregroundColor(Color.ui.darkTextMuted)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .privacySensitive()
            }
        }
        .padding()
        .background(
            RoundedRectangle(cornerRadius: 5)
                .foregroundColor(Color.ui.itemBackground)
                .frame(width: maxWidth,
                       alignment: .leading)
        )
        .frame(width: maxWidth,
               alignment: .leading)
    }
}

struct CipherItemView_Previews: PreviewProvider {
    static var previews: some View {
        CipherItemView(CipherMock.ciphers[0], .infinity)
    }
}
