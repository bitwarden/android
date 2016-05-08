using System;
using Acr.UserDialogs;
using Bit.App.Abstractions;
using Bit.App.Models.Page;
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

        private VaultViewSitePageModel Model { get; set; } = new VaultViewSitePageModel();

        private void Init()
        {
            ToolbarItems.Add(new EditSiteToolBarItem(this, _siteId));
            var stackLayout = new StackLayout();

            // Username
            var usernameRow = new StackLayout { Orientation = StackOrientation.Horizontal };
            var usernameLabel = new Label
            {
                HorizontalOptions = LayoutOptions.StartAndExpand,
                VerticalOptions = LayoutOptions.Center,
                LineBreakMode = LineBreakMode.TailTruncation
            };
            usernameLabel.SetBinding<VaultViewSitePageModel>(Label.TextProperty, s => s.Username);
            usernameRow.Children.Add(usernameLabel);
            usernameRow.Children.Add(new Button
            {
                Text = AppResources.Copy,
                HorizontalOptions = LayoutOptions.End,
                VerticalOptions = LayoutOptions.Center,
                Command = new Command(() => Copy(usernameLabel.Text, AppResources.Username))
            });
            stackLayout.Children.Add(new Label { Text = AppResources.Username });
            stackLayout.Children.Add(usernameRow);

            // Password
            var passwordRow = new StackLayout { Orientation = StackOrientation.Horizontal };
            var passwordLabel = new Label
            {
                HorizontalOptions = LayoutOptions.StartAndExpand,
                VerticalOptions = LayoutOptions.Center,
                LineBreakMode = LineBreakMode.TailTruncation
            };
            passwordLabel.SetBinding<VaultViewSitePageModel>(Label.TextProperty, s => s.MaskedPassword);
            passwordRow.Children.Add(passwordLabel);
            var togglePasswordButton = new Button
            {
                HorizontalOptions = LayoutOptions.End,
                VerticalOptions = LayoutOptions.Center,
                Command = new Command(() => Model.ShowPassword = !Model.ShowPassword)
            };
            togglePasswordButton.CommandParameter = togglePasswordButton;
            togglePasswordButton.SetBinding<VaultViewSitePageModel>(Button.TextProperty, s => s.ShowHideText);
            passwordRow.Children.Add(togglePasswordButton);
            passwordRow.Children.Add(new Button
            {
                Text = AppResources.Copy,
                HorizontalOptions = LayoutOptions.End,
                VerticalOptions = LayoutOptions.Center,
                Command = new Command(() => Copy(Model.Password, AppResources.Password))
            });
            stackLayout.Children.Add(new Label { Text = AppResources.Password });
            stackLayout.Children.Add(passwordRow);

            // URI
            var uriRow = new StackLayout { Orientation = StackOrientation.Horizontal };
            var uriLabel = new Label
            {
                HorizontalOptions = LayoutOptions.StartAndExpand,
                VerticalOptions = LayoutOptions.Center,
                LineBreakMode = LineBreakMode.TailTruncation
            };
            uriLabel.SetBinding<VaultViewSitePageModel>(Label.TextProperty, s => s.Uri);
            uriRow.Children.Add(uriLabel);
            uriRow.Children.Add(new Button
            {
                Text = AppResources.Launch,
                HorizontalOptions = LayoutOptions.End,
                VerticalOptions = LayoutOptions.Center,
                Command = new Command(() => Device.OpenUri(new Uri(uriLabel.Text)))
            });
            stackLayout.Children.Add(new Label { Text = AppResources.Website });
            stackLayout.Children.Add(uriRow);

            // Notes
            var notes = new Label { Text = AppResources.Notes };
            notes.SetBinding<VaultViewSitePageModel>(Label.IsVisibleProperty, s => s.ShowNotes);
            stackLayout.Children.Add(notes);
            var notesLabel = new Label();
            notesLabel.SetBinding<VaultViewSitePageModel>(Label.TextProperty, s => s.Notes);
            notesLabel.SetBinding<VaultViewSitePageModel>(Label.IsVisibleProperty, s => s.ShowNotes);
            stackLayout.Children.Add(notesLabel);

            var scrollView = new ScrollView
            {
                Content = stackLayout,
                Orientation = ScrollOrientation.Vertical
            };

            SetBinding(Page.TitleProperty, new Binding("PageTitle"));
            Content = scrollView;
            BindingContext = Model;
        }

        protected override void OnAppearing()
        {
            var site = _siteService.GetByIdAsync(_siteId).GetAwaiter().GetResult();
            if(site == null)
            {
                // TODO: handle error. navigate back? should never happen...
                return;
            }

            Model.Update(site);
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
