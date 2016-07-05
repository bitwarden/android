using System;
using System.Threading.Tasks;
using Acr.UserDialogs;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Resources;
using Xamarin.Forms;
using XLabs.Ioc;

namespace Bit.App.Pages
{
    public class ToolsPage : ExtendedContentPage
    {
        private readonly IUserDialogs _userDialogs;

        public ToolsPage()
        {
            _userDialogs = Resolver.Resolve<IUserDialogs>();

            Init();
        }

        public void Init()
        {
            var generatorCell = new ToolsViewCell("Password Generator", "Automatically generate strong, unique passwords for your logins.", "refresh");
            generatorCell.Tapped += GeneratorCell_Tapped;
            var extensionCell = new ToolsViewCell("bitwarden App Extension", "Use bitwarden in Safari and other apps to auto-fill your logins.", "upload");
            extensionCell.Tapped += ExtensionCell_Tapped;
            var webCell = new ToolsViewCell("bitwarden Web Vault", "Manage your logins from any web browser with the bitwarden web vault.", "globe");
            webCell.Tapped += WebCell_Tapped;
            var importCell = new ToolsViewCell("Import Logins", "Quickly bulk import your logins from other password management apps.", "cloudup");
            importCell.Tapped += ImportCell_Tapped;

            var table = new ExtendedTableView
            {
                EnableScrolling = true,
                Intent = TableIntent.Settings,
                HasUnevenRows = true,
                Root = new TableRoot
                {
                    new TableSection
                    {
                        generatorCell,
                        extensionCell,
                        webCell,
                        importCell
                    }
                }
            };

            if(Device.OS == TargetPlatform.iOS)
            {
                table.RowHeight = -1;
                table.EstimatedRowHeight = 100;
            }

            Title = AppResources.Tools;
            Content = table;
        }

        private void ExtensionCell_Tapped(object sender, EventArgs e)
        {
            Navigation.PushModalAsync(new ExtendedNavigationPage(new ToolsExtensionPage()));
        }

        private void GeneratorCell_Tapped(object sender, EventArgs e)
        {
            Navigation.PushModalAsync(new ExtendedNavigationPage(new ToolsPasswordGeneratorPage()));
        }

        private void WebCell_Tapped(object sender, EventArgs e)
        {
            Device.OpenUri(new Uri("https://vault.bitwarden.com"));
        }

        private async void ImportCell_Tapped(object sender, EventArgs e)
        {
            if(!await _userDialogs.ConfirmAsync("You can bulk import logins from the bitwarden.com web vault. Do you want to visit the website now?", null, AppResources.Yes, AppResources.Cancel))
            {
                return;
            }

            Device.OpenUri(new Uri("https://vault.bitwarden.com"));
        }

        public class ToolsViewCell : ExtendedViewCell
        {
            public ToolsViewCell(string labelText, string detailText, string imageSource)
            {
                var label = new Label
                {
                    VerticalOptions = LayoutOptions.CenterAndExpand,
                    LineBreakMode = LineBreakMode.TailTruncation,
                    Text = labelText
                };

                var detail = new Label
                {
                    FontSize = Device.GetNamedSize(NamedSize.Small, typeof(Label)),
                    LineBreakMode = LineBreakMode.WordWrap,
                    VerticalOptions = LayoutOptions.End,
                    Style = (Style)Application.Current.Resources["text-muted"],
                    Text = detailText
                };

                var labelDetailStackLayout = new StackLayout
                {
                    HorizontalOptions = LayoutOptions.StartAndExpand,
                    VerticalOptions = LayoutOptions.FillAndExpand,
                    Children = { label, detail },
                    Spacing = 0
                };

                var image = new Image
                {
                    HorizontalOptions = LayoutOptions.Start,
                    VerticalOptions = LayoutOptions.FillAndExpand,
                    Source = imageSource,
                    Margin = new Thickness(0, 0, 10, 0)
                };

                var containerStackLayout = new StackLayout
                {
                    Orientation = StackOrientation.Horizontal,
                    Children = { image, labelDetailStackLayout },
                    Padding = new Thickness(15, 25)
                };

                ShowDisclousure = true;
                View = containerStackLayout;
            }
        }
    }
}
