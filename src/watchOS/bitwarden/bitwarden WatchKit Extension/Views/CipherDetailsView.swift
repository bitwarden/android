import SwiftUI

struct CipherDetailsView: View {
    @ObservedObject var cipherDetailsViewModel: CipherDetailsViewModel
    @Environment(\.scenePhase) var scenePhase
    
    let iconSize: CGSize = CGSize(width: 30, height: 30)
    
    init(cipher: Cipher) {
        self.cipherDetailsViewModel = CipherDetailsViewModel(cipher: cipher)
    }
    
    var body: some View {
        VStack(alignment:.leading){
            HStack{
                if cipherDetailsViewModel.iconImageUri == nil {
                    iconPlaceholderImage
                } else {
                    if #available(watchOSApplicationExtension 8.0, *) {
                        AsyncImage(url: URL(string: cipherDetailsViewModel.iconImageUri!)){ phase in
                            switch phase {
                            case .empty:
                                iconPlaceholderImage
                            case .success(let image):
                                image.resizable()
                                     .aspectRatio(contentMode: .fit)
                                     .frame(maxWidth: iconSize.width, maxHeight: iconSize.height)
                            case .failure:
                                iconPlaceholderImage
                            @unknown default:
                                EmptyView()
                            }
                        }
                    } else {
                        ImageView(withURL: cipherDetailsViewModel.iconImageUri!, maxWidth: iconSize.width, maxHeight: iconSize.height) {
                            iconPlaceholderImage
                        }
                    }
                }
                Text(cipherDetailsViewModel.cipher.name!)
                    .font(.title2)
                    .fontWeight(.bold)
                    .lineLimit(1)
                    .truncationMode(.tail)
                    .padding(.leading, 5)
            }
            if cipherDetailsViewModel.cipher.login.username != nil {
                Text(cipherDetailsViewModel.cipher.login.username!)
                    .font(.title3)
                    .fontWeight(.bold)
                    .lineLimit(1)
                    .truncationMode(.tail)
                    .privacySensitive()
            }
            if cipherDetailsViewModel.totpFormatted == "" {
                ProgressView()
            } else {
                HStack{
                    let transition = AnyTransition.asymmetric(insertion: .slide, removal: .scale).combined(with: .opacity)
                    Text(cipherDetailsViewModel.totpFormatted)
                        .font(.largeTitle)
                        .scaledToFit()
                        .minimumScaleFactor(0.01)
                        .lineLimit(1)
                        .id(cipherDetailsViewModel.totpFormatted)
                        .privacySensitive()
                        .transition(transition)
                        .animation(.default.speed(0.7), value: cipherDetailsViewModel.totpFormatted)
                    Spacer()
                    ZStack{
                        CircularProgressView(progress: cipherDetailsViewModel.progress, strokeLineWidth: 3, strokeColor: Color.blue, endingStrokeColor: Color.red)
                            .frame(width: 40, height:40)
                        Text("\(cipherDetailsViewModel.counter)")
                            .font(.title3)
                            .fontWeight(.semibold)
                            .privacySensitive()
                    }
                }
                .padding(.top, 20)
                .padding(.leading, 5)
                .padding(.trailing, 5)
            }
        }
        .onAppear{
            self.cipherDetailsViewModel.startGeneration()
        }
        .onDisappear{
            self.cipherDetailsViewModel.stopGeneration()
        }
        .onChange(of: scenePhase) { newPhase in
            if newPhase == .active {
                try? self.cipherDetailsViewModel.regenerateTotp()
            }
        }
    }
    
    var iconPlaceholderImage: some View{
        Image("DefaultCipherIcon")
            .resizable()
            .aspectRatio(contentMode: .fit)
            .frame(maxWidth: iconSize.width, maxHeight: iconSize.height)

    }
}

struct CipherDetailsView_Previews: PreviewProvider {
    static var previews: some View {
        CipherDetailsView(cipher: CipherMock.ciphers[0])
    }
}
