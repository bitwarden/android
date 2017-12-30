using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class FabLayout : AbsoluteLayout
    {
        public FabLayout(View mainView)
        {
            VerticalOptions = LayoutOptions.FillAndExpand;
            HorizontalOptions = LayoutOptions.FillAndExpand;
            SetLayoutFlags(mainView, AbsoluteLayoutFlags.All);
            SetLayoutBounds(mainView, new Rectangle(0, 0, 1, 1));
            Children.Add(mainView);
        }
    }
}
