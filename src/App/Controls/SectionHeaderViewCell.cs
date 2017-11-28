using Bit.App.Utilities;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class SectionHeaderViewCell : ExtendedViewCell
    {
        public SectionHeaderViewCell(string bindingName, string countBindingName = null, Thickness? padding = null)
        {
            var label = new Label
            {
                FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label)),
                Style = (Style)Application.Current.Resources["text-muted"],
                VerticalTextAlignment = TextAlignment.Center,
                HorizontalOptions = LayoutOptions.StartAndExpand
            };

            label.SetBinding(Label.TextProperty, bindingName);

            var stackLayout = new StackLayout
            {
                Padding = padding ?? new Thickness(16, 8),
                Children = { label },
                Orientation = StackOrientation.Horizontal
            };

            if(!string.IsNullOrWhiteSpace(countBindingName))
            {
                var countLabel = new Label
                {
                    LineBreakMode = LineBreakMode.NoWrap,
                    FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label)),
                    Style = (Style)Application.Current.Resources["text-muted"],
                    HorizontalOptions = LayoutOptions.End,
                    VerticalTextAlignment = TextAlignment.Center
                };
                countLabel.SetBinding(Label.TextProperty, countBindingName);
                stackLayout.Children.Add(countLabel);
            }

            View = stackLayout;
            BackgroundColor = Color.FromHex("efeff4");
        }
    }
}
