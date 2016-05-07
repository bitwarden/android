using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection.Emit;
using System.Text;
using Acr.UserDialogs;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Xamarin.Forms;
using XLabs.Ioc;

namespace Bit.App.Pages
{
    public class VaultViewSitePage : ContentPage
    {
        private readonly string _siteId;
        private readonly ISiteService _siteService;
        private readonly IUserDialogs _userDialogs;
        private readonly IClipboardService _clipboardService;

        public VaultViewSitePage(string siteId)
        {
            _siteId = siteId;
            _siteService = Resolver.Resolve<ISiteService>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _clipboardService = Resolver.Resolve<IClipboardService>();

            Init();
        }

        public void Init()
        {
            ToolbarItems.Add(new EditSiteToolBarItem(this, _siteId));

            var site = _siteService.GetByIdAsync(_siteId).GetAwaiter().GetResult();
            if(site == null)
            {
                // TODO: handle error. navigate back? should never happen...
                return;
            }

            var usernameRow = new StackLayout { Orientation = StackOrientation.Horizontal };
            var usernameLabel = new Label
            {
                Text = site.Username?.Decrypt(),
                HorizontalOptions = LayoutOptions.StartAndExpand,
                VerticalOptions = LayoutOptions.Center,
                LineBreakMode = LineBreakMode.TailTruncation
            };
            usernameRow.Children.Add(usernameLabel);
            usernameRow.Children.Add(new Button
            {
                Text = AppResources.Copy,
                HorizontalOptions = LayoutOptions.End,
                VerticalOptions = LayoutOptions.Center,
                Command = new Command(() => Copy(usernameLabel.Text, AppResources.Username))
            });

            var passwordRow = new StackLayout { Orientation = StackOrientation.Horizontal };
            var password = site.Password?.Decrypt();
            var passwordLabel = new Label
            {
                Text = new string('●', password.Length),
                HorizontalOptions = LayoutOptions.StartAndExpand,
                VerticalOptions = LayoutOptions.Center,
                LineBreakMode = LineBreakMode.TailTruncation
            };
            passwordRow.Children.Add(passwordLabel);
            var togglePasswordButton = new Button
            {
                Text = AppResources.Show,
                HorizontalOptions = LayoutOptions.End,
                VerticalOptions = LayoutOptions.Center,
                Command = new Command((self) => TogglePassword(self as Button, passwordLabel, password))
            };
            togglePasswordButton.CommandParameter = togglePasswordButton;
            passwordRow.Children.Add(togglePasswordButton);
            passwordRow.Children.Add(new Button
            {
                Text = AppResources.Copy,
                HorizontalOptions = LayoutOptions.End,
                VerticalOptions = LayoutOptions.Center,
                Command = new Command(() => Copy(password, AppResources.Password))
            });

            var uriRow = new StackLayout { Orientation = StackOrientation.Horizontal };
            var uri = site.Uri?.Decrypt();
            uriRow.Children.Add(new Label
            {
                Text = uri,
                HorizontalOptions = LayoutOptions.StartAndExpand,
                VerticalOptions = LayoutOptions.Center,
                LineBreakMode = LineBreakMode.TailTruncation
            });
            uriRow.Children.Add(new Button
            {
                Text = AppResources.Launch,
                HorizontalOptions = LayoutOptions.End,
                VerticalOptions = LayoutOptions.Center,
                Command = new Command(() => Device.OpenUri(new Uri(uri)))
            });

            var stackLayout = new StackLayout();
            stackLayout.Children.Add(new Label { Text = AppResources.Username });
            stackLayout.Children.Add(usernameRow);
            stackLayout.Children.Add(new Label { Text = AppResources.Password });
            stackLayout.Children.Add(passwordRow);
            stackLayout.Children.Add(new Label { Text = AppResources.Website });
            stackLayout.Children.Add(uriRow);
            if(site.Notes != null)
            {
                stackLayout.Children.Add(new Label { Text = AppResources.Notes });
                stackLayout.Children.Add(new Label { Text = site.Notes.Decrypt() });
            }

            var scrollView = new ScrollView
            {
                Content = stackLayout,
                Orientation = ScrollOrientation.Vertical
            };

            Title = site.Name?.Decrypt() ?? AppResources.SiteNoName;
            Content = scrollView;
        }

        private void TogglePassword(Button toggleButton, Label passwordLabel, string password)
        {
            if(toggleButton.Text == AppResources.Show)
            {
                toggleButton.Text = AppResources.Hide;
                passwordLabel.Text = password;
            }
            else
            {
                toggleButton.Text = AppResources.Show;
                passwordLabel.Text = new string('●', password.Length);
            }
        }

        private void Copy(string copyText, string alertLabel)
        {
            _clipboardService.CopyToClipboard(copyText);
            _userDialogs.SuccessToast(string.Format(AppResources.ValueHasBeenCopied, alertLabel));
        }

        private class EditSiteToolBarItem : ToolbarItem
        {
            private readonly VaultViewSitePage _page;
            private readonly string _siteId;

            public EditSiteToolBarItem(VaultViewSitePage page, string siteId)
            {
                _page = page;
                _siteId = siteId;
                Text = AppResources.Edit;
                Clicked += ClickedItem;
            }

            private async void ClickedItem(object sender, EventArgs e)
            {
                await _page.Navigation.PushAsync(new VaultEditSitePage(_siteId));
            }
        }
    }
}
