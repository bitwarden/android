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

            Button = new ExtendedButton
            {
                WidthRequest = 60
            };

            var grid = new Grid
            {
                ColumnSpacing = 0,
                RowSpacing = 0,
                Padding = new Thickness(15, 3, 0, 3)
            };
            grid.RowDefinitions.Add(new RowDefinition { Height = new GridLength(1, GridUnitType.Star) });
            grid.RowDefinitions.Add(new RowDefinition { Height = new GridLength(1, GridUnitType.Star) });
            grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
            grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(60, GridUnitType.Absolute) });
            grid.Children.Add(Label, 0, 0);
            grid.Children.Add(Detail, 0, 1);
            grid.Children.Add(Button, 1, 0);
            Grid.SetRowSpan(Button, 2);

            if(Device.OS == TargetPlatform.Android)
            {
                Label.TextColor = Color.Black;
            }

            View = grid;
        }

        public Label Label { get; private set; }
        public Label Detail { get; private set; }
        public Button Button { get; private set; }
    }
}
