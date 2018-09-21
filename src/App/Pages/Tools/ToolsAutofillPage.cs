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
            var getAccessLabel = new Label
            {
                Text = AppResources.ExtensionInstantAccess,
                VerticalOptions = LayoutOptions.Start,
                HorizontalOptions = LayoutOptions.Center,
                HorizontalTextAlignment = TextAlignment.Center,
                LineBreakMode = LineBreakMode.WordWrap,
                FontSize = Device.GetNamedSize(NamedSize.Large, typeof(Label)),
                Margin = new Thickness(0, 0, 0, 20),
            };

            var turnOnLabel = new Label
            {
                Text = AppResources.AutofillTurnOn,
                VerticalOptions = LayoutOptions.Start,
                HorizontalOptions = LayoutOptions.Center,
                HorizontalTextAlignment = TextAlignment.Center,
                LineBreakMode = LineBreakMode.WordWrap
            };

            var turnOnLabel1 = new Label
            {
                Text = AppResources.AutofillTurnOn1,
                VerticalOptions = LayoutOptions.Start,
                HorizontalOptions = LayoutOptions.Start,
                HorizontalTextAlignment = TextAlignment.Start,
                LineBreakMode = LineBreakMode.WordWrap
            };

            var turnOnLabel2 = new Label
            {
                Text = AppResources.AutofillTurnOn2,
                VerticalOptions = LayoutOptions.Start,
                HorizontalOptions = LayoutOptions.Start,
                HorizontalTextAlignment = TextAlignment.Start,
                LineBreakMode = LineBreakMode.WordWrap
            };

            var turnOnLabel3 = new Label
            {
                Text = AppResources.AutofillTurnOn3,
                VerticalOptions = LayoutOptions.Start,
                HorizontalOptions = LayoutOptions.Start,
                HorizontalTextAlignment = TextAlignment.Start,
                LineBreakMode = LineBreakMode.WordWrap
            };

            var turnOnLabel4 = new Label
            {
                Text = AppResources.AutofillTurnOn4,
                VerticalOptions = LayoutOptions.Start,
                HorizontalOptions = LayoutOptions.Start,
                HorizontalTextAlignment = TextAlignment.Start,
                LineBreakMode = LineBreakMode.WordWrap
            };

            var turnOnLabel5 = new Label
            {
                Text = AppResources.AutofillTurnOn5,
                VerticalOptions = LayoutOptions.Start,
                HorizontalOptions = LayoutOptions.Start,
                HorizontalTextAlignment = TextAlignment.Start,
                LineBreakMode = LineBreakMode.WordWrap
            };

            var keyboardImge = new CachedImage
            {
                Source = "autofill-kb.png",
                VerticalOptions = LayoutOptions.CenterAndExpand,
                HorizontalOptions = LayoutOptions.Center,
                Margin = new Thickness(0, 10, 0, 0),
                WidthRequest = 290,
                HeightRequest = 252
            };

            var stackLayout = new StackLayout
            {
                Orientation = StackOrientation.Vertical,
                Spacing = 5,
                Padding = new Thickness(20, 20, 20, 30),
                Children = { getAccessLabel, turnOnLabel, turnOnLabel1, turnOnLabel2,
                    turnOnLabel3, turnOnLabel4, turnOnLabel5, keyboardImge },
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
