using System;
using Bit.App.Controls;
using Xamarin.Forms;
using Bit.App.Resources;
using Bit.App.Utilities;

namespace Bit.App.Pages
{
    public class SettingsCreditsPage : ExtendedContentPage
    {
        public SettingsCreditsPage()
        {
            Init();
        }

        public void Init()
        {
            var table = new ExtendedTableView
            {
                EnableScrolling = true,
                Intent = TableIntent.Settings,
                HasUnevenRows = true,
                EnableSelection = false,
                Root = new TableRoot
                {
                    new TableSection(AppResources.Translations)
                    {
                        new CustomViewCell(@"@felixqu - Chinese Simplified
@thomassth - Chinese Traditional
@Primokorn, @maxlandry - French
@bestHippos - Italian
@SW1FT - Portuguese
@majod - Slovak
@King-Tut-Tut - Swedish
@Igetin - Finnish")
                    },
                    new TableSection(AppResources.Icons)
                    {
                        new CustomViewCell(@"Tools by Alex Auda Samora from the Noun Project
Fingerprint by masterpage.com from the Noun Project")
                    }
                }
            };

            if(Device.RuntimePlatform == Device.iOS)
            {
                table.RowHeight = -1;
                table.EstimatedRowHeight = 100;
            }

            Title = AppResources.ThankYou;
            Content = table;
        }

        public class CustomViewCell : ViewCell
        {
            public CustomViewCell(string text)
            {
                var label = new Label
                {
                    LineBreakMode = LineBreakMode.WordWrap,
                    Text = text,
                    FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label))
                };

                var layout = new StackLayout
                {
                    Children = { label },
                    Padding = Helpers.OnPlatform(
                        iOS: new Thickness(15, 20),
                        Android: new Thickness(16, 20),
                        WinPhone: new Thickness(15, 20)),
                    BackgroundColor = Color.White
                };

                if(Device.RuntimePlatform == Device.Android)
                {
                    label.TextColor = Color.Black;
                }
                
                View = layout;
            }
        }
    }
}
