using FFImageLoading.Forms;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class LabeledRightDetailCell : ExtendedViewCell
    {
        public LabeledRightDetailCell(bool showIcon = true)
        {
            Label = new Label
            {
                LineBreakMode = LineBreakMode.TailTruncation,
                FontSize = Device.GetNamedSize(NamedSize.Medium, typeof(Label)),
                HorizontalOptions = LayoutOptions.StartAndExpand,
            };

            Detail = new Label
            {
                FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label)),
                Style = (Style)Application.Current.Resources["text-muted"],
                HorizontalOptions = LayoutOptions.End,
                VerticalOptions = LayoutOptions.Center
            };

            StackLayout = new StackLayout
            {
                Orientation = StackOrientation.Horizontal,
                Padding = new Thickness(15, 10),
                Children = { Label, Detail }
            };

            if(showIcon)
            {
                Icon = new CachedImage
                {
                    WidthRequest = 16,
                    HeightRequest = 16,
                    HorizontalOptions = LayoutOptions.End,
                    VerticalOptions = LayoutOptions.Center,
                    Margin = new Thickness(5, 0, 0, 0)
                };

                StackLayout.Children.Add(Icon);
            }

            if(Device.RuntimePlatform == Device.Android)
            {
                Label.TextColor = Color.Black;
            }

            View = StackLayout;
        }

        public Label Label { get; private set; }
        public Label Detail { get; private set; }
        public CachedImage Icon { get; private set; }
        public StackLayout StackLayout { get; private set; }
    }
}
