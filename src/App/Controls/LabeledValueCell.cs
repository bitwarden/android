using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class LabeledValueCell : ExtendedViewCell
    {
        public LabeledValueCell(
            string labelText = null,
            string valueText = null,
            string button1Text = null,
            string button2Text = null)
        {
            var containerStackLayout = new StackLayout
            {
                Padding = new Thickness(15),
                Orientation = StackOrientation.Horizontal
            };

            var labelValueStackLayout = new StackLayout
            {
                HorizontalOptions = LayoutOptions.StartAndExpand,
                VerticalOptions = LayoutOptions.Center
            };

            if(labelText != null)
            {
                Label = new Label
                {
                    Text = labelText,
                    FontSize = 14,
                    TextColor = Color.FromHex("777777"),
                    VerticalOptions = LayoutOptions.Start
                };

                labelValueStackLayout.Children.Add(Label);
            }

            Value = new Label
            {
                Text = valueText,
                LineBreakMode = LineBreakMode.TailTruncation,
                VerticalOptions = LayoutOptions.CenterAndExpand,
                Margin = new Thickness(0, 5, 0, 0)
            };

            labelValueStackLayout.Children.Add(Value);

            containerStackLayout.Children.Add(labelValueStackLayout);

            var buttonStackLayout = new StackLayout
            {
                Orientation = StackOrientation.Horizontal
            };

            if(button1Text != null)
            {
                Button1 = new Button
                {
                    Text = button1Text,
                    HorizontalOptions = LayoutOptions.End,
                    VerticalOptions = LayoutOptions.Center
                };

                buttonStackLayout.Children.Add(Button1);
            }

            if(button2Text != null)
            {
                Button2 = new Button
                {
                    Text = button2Text,
                    HorizontalOptions = LayoutOptions.End,
                    VerticalOptions = LayoutOptions.Center
                };

                buttonStackLayout.Children.Add(Button2);
            }

            containerStackLayout.Children.Add(buttonStackLayout);

            View = containerStackLayout;
        }

        public Label Label { get; private set; }
        public Label Value { get; private set; }
        public Button Button1 { get; private set; }
        public Button Button2 { get; private set; }
    }
}
