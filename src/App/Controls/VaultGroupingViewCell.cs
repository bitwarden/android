using Bit.App.Models.Page;
using FFImageLoading.Forms;
using Xamarin.Forms;

namespace Bit.App.Controls
{
    public class VaultGroupingViewCell : ExtendedViewCell
    {
        public VaultGroupingViewCell()
        {
            Icon = new CachedImage
            {
                WidthRequest = 20,
                HeightRequest = 20,
                HorizontalOptions = LayoutOptions.Center,
                VerticalOptions = LayoutOptions.Center,
                Source = "folder.png",
                Margin = new Thickness(0, 0, 10, 0)
            };

            Label = new Label
            {
                LineBreakMode = LineBreakMode.TailTruncation,
                FontSize = Device.GetNamedSize(NamedSize.Medium, typeof(Label)),
                HorizontalOptions = LayoutOptions.StartAndExpand
            };
            Label.SetBinding(Label.TextProperty, string.Format("{0}.{1}",
                nameof(VaultListPageModel.GroupingOrCipher.Grouping), nameof(VaultListPageModel.Grouping.Name)));

            CountLabel = new Label
            {
                LineBreakMode = LineBreakMode.NoWrap,
                FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label)),
                Style = (Style)Application.Current.Resources["text-muted"],
                HorizontalOptions = LayoutOptions.End
            };
            CountLabel.SetBinding(Label.TextProperty, string.Format("{0}.{1}",
                nameof(VaultListPageModel.GroupingOrCipher.Grouping), nameof(VaultListPageModel.Grouping.Count)));

            var stackLayout = new StackLayout
            {
                Spacing = 0,
                Padding = new Thickness(16, 10),
                Children = { Icon, Label, CountLabel },
                Orientation = StackOrientation.Horizontal
            };

            if(Device.RuntimePlatform == Device.Android)
            {
                Label.TextColor = Color.Black;
            }

            View = stackLayout;
            BackgroundColor = Color.White;
        }

        public CachedImage Icon { get; private set; }
        public Label Label { get; private set; }
        public Label CountLabel { get; private set; }

        protected override void OnBindingContextChanged()
        {
            if(BindingContext is VaultListPageModel.GroupingOrCipher model)
            {
                Icon.Source = model.Grouping.Folder ?
                    $"folder{(model.Grouping.Id == null ? "_o" : string.Empty)}.png" : "cube.png";
            }

            base.OnBindingContextChanged();
        }
    }
}
