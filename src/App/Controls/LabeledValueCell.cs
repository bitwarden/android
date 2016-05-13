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
        private readonly IUserDialogs _userDialogs;
        private readonly IClipboardService _clipboardService;

        public LabeledValueCell(
            string labelText,
            string valueText = null,
            bool copyValue = false,
            bool password = false,
            bool launch = false)
        {
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _clipboardService = Resolver.Resolve<IClipboardService>();

            var stackLayout = new StackLayout
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

                stackLayout.Children.Add(Label);
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

            if(copyValue)
            {
                var copyButton = new Button
                {
                    Text = AppResources.Copy,
                    HorizontalOptions = LayoutOptions.End,
                    VerticalOptions = LayoutOptions.Center,
                    Command = new Command(() => Copy())
                };

                valueStackLayout.Children.Add(copyButton);
            }

            if(launch)
            {
                var launchButton = new Button
                {
                    Text = AppResources.Launch,
                    HorizontalOptions = LayoutOptions.End,
                    VerticalOptions = LayoutOptions.Center,
                    Command = new Command(() => Device.OpenUri(new Uri(Value.Text)))
                };

                valueStackLayout.Children.Add(launchButton);
            }

            stackLayout.Children.Add(valueStackLayout);

            View = stackLayout;
        }

        public Label Label { get; private set; }
        public Label Value { get; private set; }

        private void Copy()
        {
            _clipboardService.CopyToClipboard(Value.Text);
            _userDialogs.SuccessToast(string.Format(AppResources.ValueHasBeenCopied, Label.Text));
        }
    }
}
