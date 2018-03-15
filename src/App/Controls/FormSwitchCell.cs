using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class FormSwitchCell : ExtendedViewCell
    {
        public FormSwitchCell(string labelText, string button1 = null)
        {
            Label = new Label
            {
                Text = labelText,
                HorizontalOptions = LayoutOptions.FillAndExpand,
                VerticalTextAlignment = TextAlignment.Center,
                TextColor = Color.Black,
                FontSize = Device.GetNamedSize(NamedSize.Medium, typeof(Label)),
            };
            Switch = new Switch
            {
                VerticalOptions = LayoutOptions.Center
            };

            var stackLayout = new StackLayout
            {
                Padding = new Thickness(15, 5),
                Orientation = StackOrientation.Horizontal,
                Children = { Label, Switch }
            };
            stackLayout.AdjustPaddingForDevice();

            if(!string.IsNullOrWhiteSpace(button1))
            {
                Button1 = new ExtendedButton { Image = button1 };
                stackLayout.Children.Add(Button1);
                Button1.BackgroundColor = Color.Transparent;
                Button1.Padding = new Thickness(0);
                Button1.WidthRequest = 40;
                Button1.VerticalOptions = LayoutOptions.FillAndExpand;
            }

            View = stackLayout;
        }

        public Switch Switch { get; private set; }
        public Label Label { get; set; }
        public ExtendedButton Button1 { get; set; }
    }
}
