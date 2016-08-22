using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class LabeledDetailCell : ExtendedViewCell
    {
        public LabeledDetailCell()
        {
            Label = new Label
            {
                LineBreakMode = LineBreakMode.TailTruncation,
                FontSize = Device.GetNamedSize(NamedSize.Medium, typeof(Label))
            };

            Detail = new Label
            {
                FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label)),
                LineBreakMode = LineBreakMode.TailTruncation,
                Style = (Style)Application.Current.Resources["text-muted"]
            };

            var labelDetailStackLayout = new StackLayout
            {
                HorizontalOptions = LayoutOptions.StartAndExpand,
                VerticalOptions = LayoutOptions.CenterAndExpand,
                Children = { Label, Detail },
                Padding = new Thickness(15, 5, 5, 5),
                Spacing = 0
            };

            Button = new Button
            {
                HorizontalOptions = LayoutOptions.End,
                VerticalOptions = LayoutOptions.CenterAndExpand,
                WidthRequest = 50
            };

            var containerStackLayout = new StackLayout
            {
                Orientation = StackOrientation.Horizontal,
                Children = { labelDetailStackLayout, Button }
            };

            if(Device.OS == TargetPlatform.Android)
            {
                Label.TextColor = Color.Black;
            }

            View = containerStackLayout;
        }

        public Label Label { get; private set; }
        public Label Detail { get; private set; }
        public Button Button { get; private set; }
    }
}
