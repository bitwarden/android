using System;
using Bit.App.Controls;
using Xamarin.Forms;
using Bit.App.Resources;
using FFImageLoading.Forms;

namespace Bit.App.Pages
{
    public class ToolsAutofillPage : ExtendedContentPage
    {
        public ToolsAutofillPage()
        {
            Init();
        }

        public void Init()
        {
            var notStartedLabel = new Label
            {
                Text = AppResources.ExtensionInstantAccess,
                VerticalOptions = LayoutOptions.Start,
                HorizontalOptions = LayoutOptions.Center,
                HorizontalTextAlignment = TextAlignment.Center,
                LineBreakMode = LineBreakMode.WordWrap,
                FontSize = Device.GetNamedSize(NamedSize.Large, typeof(Label))
            };

            var notStartedSublabel = new Label
            {
                Text = AppResources.ExtensionTurnOn,
                VerticalOptions = LayoutOptions.Start,
                HorizontalOptions = LayoutOptions.Center,
                HorizontalTextAlignment = TextAlignment.Center,
                LineBreakMode = LineBreakMode.WordWrap
            };

            var keyboardImge = new CachedImage
            {
                Source = "autofill-kb.png",
                VerticalOptions = LayoutOptions.CenterAndExpand,
                HorizontalOptions = LayoutOptions.Center,
                Margin = new Thickness(0, -10, 0, 0),
                WidthRequest = 290,
                HeightRequest = 252
            };

            var stackLayout = new StackLayout
            {
                Orientation = StackOrientation.Vertical,
                Spacing = 20,
                Padding = new Thickness(20, 20, 20, 30),
                Children = { notStartedLabel, notStartedSublabel, keyboardImge },
                VerticalOptions = LayoutOptions.FillAndExpand
            };

            if(Device.RuntimePlatform == Device.iOS)
            {
                ToolbarItems.Add(new DismissModalToolBarItem(this, AppResources.Close));
            }

            Title = AppResources.Autofill;
            Content = new ScrollView { Content = stackLayout };
        }
    }
}
