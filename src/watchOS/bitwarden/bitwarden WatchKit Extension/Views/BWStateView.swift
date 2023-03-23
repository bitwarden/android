import SwiftUI

struct BWStateView: View {
    @ObservedObject var viewModel:BWStateViewModel
    
    init(_ state: BWState, _ defaultText: String?) {
        viewModel = BWStateViewModel(state, defaultText)
    }
    
    var body: some View {
        VStack(alignment: .center) {
            Spacer()
            Image("BitwardenImagetype")
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: .infinity, height: 35)
                .padding(.leading, 15)
                .padding(.trailing, 15)
                .padding(.top, 5)
            Spacer()
            Text(LocalizedStringKey(viewModel.text))
                .font(.title3)
                .fontWeight(.semibold)
                .multilineTextAlignment(.center)
            if viewModel.isLoading {
                ProgressView()
                    .frame(width: 20, height: 20)
            }
        }
    }
}

struct BWStateView_Previews: PreviewProvider {
    static var previews: some View {
        BWStateView(.needSetup, nil)
    }
}
