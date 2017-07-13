using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class LabeledValueCell : ExtendedViewCell
    {
        public LabeledValueCell(
            string labelText = null,
            string valueText = null,
            string button1Text = null,
            string button2Text = null,
            string subText = null)
        {
            var containerStackLayout = new StackLayout
            {
                Padding = new Thickness(15, 10),
                Orientation = StackOrientation.Horizontal
            };

            var labelValueStackLayout = new StackLayout
            {
                HorizontalOptions = LayoutOptions.StartAndExpand,
                VerticalOptions = LayoutOptions.CenterAndExpand
            };

            if(labelText != null)
            {
                Label = new Label
                {
                    Text = labelText,
                    FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label)),
                    Style = (Style)Application.Current.Resources["text-muted"]
                };

                labelValueStackLayout.Children.Add(Label);
            }

            Value = new Label
            {
                Text = valueText,
                FontSize = Device.GetNamedSize(NamedSize.Medium, typeof(Label)),
                LineBreakMode = LineBreakMode.TailTruncation
            };

            if(Device.RuntimePlatform == Device.Android)
            {
                Value.TextColor = Color.Black;
            }

            labelValueStackLayout.Children.Add(Value);

            containerStackLayout.Children.Add(labelValueStackLayout);

            var buttonStackLayout = new StackLayout
            {
                Orientation = StackOrientation.Horizontal,
                VerticalOptions = LayoutOptions.CenterAndExpand
            };

            if(subText != null)
            {
                Sub = new Label
                {
                    FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label)),
                    HorizontalOptions = LayoutOptions.End,
                    VerticalOptions = LayoutOptions.Center
                };

                buttonStackLayout.Children.Add(Sub);
            }

            if(button1Text != null)
            {
                Button1 = new ExtendedButton
                {
                    Text = button1Text,
                    HorizontalOptions = LayoutOptions.End,
                    VerticalOptions = LayoutOptions.Center,
                    Margin = new Thickness(0)
                };

                buttonStackLayout.Children.Add(Button1);
            }

            if(button2Text != null)
            {
                Button2 = new ExtendedButton
                {
                    Text = button2Text,
                    HorizontalOptions = LayoutOptions.End,
                    VerticalOptions = LayoutOptions.Center,
                    Margin = new Thickness(0)
                };

                buttonStackLayout.Children.Add(Button2);
            }

            if(Device.RuntimePlatform == Device.Android)
            {
                buttonStackLayout.Spacing = 5;

                if(Button1 != null)
                {
                    Button1.Padding = new Thickness(0);
                    Button1.BackgroundColor = Color.Transparent;
                }
                if(Button2 != null)
                {
                    Button2.Padding = new Thickness(0);
                    Button2.BackgroundColor = Color.Transparent;
                }

                containerStackLayout.AdjustPaddingForDevice();
            }

            if(Sub != null && Button1 != null)
            {
                Sub.Margin = new Thickness(0, 0, 10, 0);
            }

            containerStackLayout.Children.Add(buttonStackLayout);
            View = containerStackLayout;
        }

        public Label Label { get; private set; }
        public Label Value { get; private set; }
        public Label Sub { get; private set; }
        public ExtendedButton Button1 { get; private set; }
        public ExtendedButton Button2 { get; private set; }
    }
}
