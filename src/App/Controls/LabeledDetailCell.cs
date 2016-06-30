using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class LabeledDetailCell : ViewCell
    {
        public LabeledDetailCell()
        {
            Label = new Label
            {
                VerticalOptions = LayoutOptions.CenterAndExpand,
                LineBreakMode = LineBreakMode.TailTruncation
            };

            Detail = new Label
            {
                FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label)),
                LineBreakMode = LineBreakMode.TailTruncation,
                VerticalOptions = LayoutOptions.End,
                Style = (Style)Application.Current.Resources["text-muted"],
            };

            var labelDetailStackLayout = new StackLayout
            {
                HorizontalOptions = LayoutOptions.StartAndExpand,
                VerticalOptions = LayoutOptions.FillAndExpand,
                Children = { Label, Detail },
                Padding = new Thickness(15, 5, 5, 5),
                Spacing = 0
            };

            Button = new Button
            {
                HorizontalOptions = LayoutOptions.End,
                VerticalOptions = LayoutOptions.FillAndExpand,
                WidthRequest = 50
            };

            var containerStackLayout = new StackLayout
            {
                Orientation = StackOrientation.Horizontal,
                Children = { labelDetailStackLayout, Button }
            };

            View = containerStackLayout;
        }

        public Label Label { get; private set; }
        public Label Detail { get; private set; }
        public Button Button { get; private set; }
    }
}
