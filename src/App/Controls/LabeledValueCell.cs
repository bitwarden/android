using Acr.UserDialogs;
using Bit.App.Abstractions;
using Bit.App.Resources;
using System;
using Xamarin.Forms;
using XLabs.Ioc;

namespace Bit.App.Controls
{
    public class LabeledValueCell : ViewCell
    {
        public LabeledValueCell(
            string labelText = null,
            string valueText = null,
            string button1Text = null,
            string button2Text = null)
        {
            StackLayout = new StackLayout
            {
                Padding = new Thickness(15, 15, 15, 0),
                BackgroundColor = Color.White
            };

            if(labelText != null)
            {
                Label = new Label
                {
                    Text = labelText,
                    FontSize = 14,
                    TextColor = Color.FromHex("777777")
                };

                StackLayout.Children.Add(Label);
            }

            Value = new Label
            {
                Text = valueText,
                LineBreakMode = LineBreakMode.TailTruncation,
                HorizontalOptions = LayoutOptions.StartAndExpand,
                VerticalOptions = LayoutOptions.Center
            };

            var valueStackLayout = new StackLayout
            {
                Orientation = StackOrientation.Horizontal
            };

            valueStackLayout.Children.Add(Value);

            if(button1Text != null)
            {
                Button1 = new Button
                {
                    Text = button1Text,
                    HorizontalOptions = LayoutOptions.End,
                    VerticalOptions = LayoutOptions.Center
                };

                valueStackLayout.Children.Add(Button1);
            }

            if(button2Text != null)
            {
                Button2 = new Button
                {
                    Text = button2Text,
                    HorizontalOptions = LayoutOptions.End,
                    VerticalOptions = LayoutOptions.Center
                };

                valueStackLayout.Children.Add(Button2);
            }

            StackLayout.Children.Add(valueStackLayout);

            View = StackLayout;
        }

        public StackLayout StackLayout { get; private set; }
        public Label Label { get; private set; }
        public Label Value { get; private set; }
        public Button Button1 { get; private set; }
        public Button Button2 { get; private set; }
    }
}
