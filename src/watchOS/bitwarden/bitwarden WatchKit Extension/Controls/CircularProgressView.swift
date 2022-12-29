import SwiftUI

struct CircularProgressView: View {
    let progress: Double
    let strokeLineWidth:CGFloat
    let strokeColor:Color
    let endingStrokeColor:Color
    
    var currentColor: Color{
        return progress > 0.2 ? strokeColor : endingStrokeColor
    }
    
    var body: some View {
        ZStack {
            Circle()
                .stroke(
                    currentColor.opacity(0.5),
                    lineWidth: strokeLineWidth
                )
            Circle()
                .trim(from: 0, to: progress)
                .stroke(
                    currentColor,
                    style: StrokeStyle(
                        lineWidth: strokeLineWidth,
                        lineCap: .round
                    )
                )
                .rotationEffect(.degrees(-90))
                .animation(.easeOut, value: progress)
        }
    }
}

struct CircularProgressView_Previews: PreviewProvider {
    static var previews: some View {
        CircularProgressView(progress:0.5, strokeLineWidth:5, strokeColor: Color.blue, endingStrokeColor: Color.red)
    }
}
