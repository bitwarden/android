using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class LabeledDetailCell : ExtendedViewCell
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

            var stackLayout = new StackLayout
            {
                Padding = new Thickness(20, 5),
                HorizontalOptions = LayoutOptions.FillAndExpand,
                VerticalOptions = LayoutOptions.FillAndExpand,
                Children = { Label, Detail },
                Spacing = 0
            };

            View = stackLayout;
        }

        public Label Label { get; private set; }
        public Label Detail { get; private set; }
    }
}
