using System;
using System.Threading.Tasks;
using Acr.UserDialogs;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Xamarin.Forms;
using XLabs.Ioc;
using Bit.App.Controls;
using System.Linq;
using Plugin.Settings.Abstractions;

namespace Bit.App.Pages
{
    public class LockPasswordPage : ExtendedContentPage
    {
        private readonly IAuthService _authService;
        private readonly ISettings _settings;
        private readonly IUserDialogs _userDialogs;
        private readonly ICryptoService _cryptoService;

        public LockPasswordPage()
            : base(false)
        {
            _authService = Resolver.Resolve<IAuthService>();
            _settings = Resolver.Resolve<ISettings>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _cryptoService = Resolver.Resolve<ICryptoService>();

            Init();
        }

        public FormEntryCell PasswordCell { get; set; }

        public void Init()
        {
            var padding = Device.OnPlatform(
                iOS: new Thickness(15, 20),
                Android: new Thickness(15, 8),
                WinPhone: new Thickness(15, 20));

            PasswordCell = new FormEntryCell(AppResources.MasterPassword, IsPassword: true,
                useLabelAsPlaceholder: true, imageSource: "lock", containerPadding: padding);

            PasswordCell.Entry.ReturnType = Enums.ReturnType.Go;
            PasswordCell.Entry.Completed += Entry_Completed;

            var table = new ExtendedTableView
            {
                Intent = TableIntent.Settings,
                EnableScrolling = false,
                HasUnevenRows = true,
                EnableSelection = false,
                VerticalOptions = LayoutOptions.Start,
                NoFooter = true,
                Root = new TableRoot
                {
                    new TableSection
                    {
                        PasswordCell
                    }
                }
            };

            var logoutButton = new Button
            {
                Text = AppResources.LogOut,
                Command = new Command(async () => await LogoutAsync()),
                VerticalOptions = LayoutOptions.End,
                Style = (Style)Application.Current.Resources["btn-primaryAccent"]
            };

            var stackLayout = new StackLayout
            {
                Spacing = 10,
                Children = { table, logoutButton }
            };

            var loginToolbarItem = new ToolbarItem("Submit", null, async () =>
            {
                await CheckPasswordAsync();
            }, ToolbarItemOrder.Default, 0);

            ToolbarItems.Add(loginToolbarItem);
            Title = "Verify Master Password";
            Content = stackLayout;
        }

        private void Entry_Completed(object sender, EventArgs e)
        {
            var task = CheckPasswordAsync();
        }

        protected override bool OnBackButtonPressed()
        {
            return true;
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            PasswordCell.Entry.Focus();
        }

        protected async Task CheckPasswordAsync()
        {
            if(string.IsNullOrWhiteSpace(PasswordCell.Entry.Text))
            {
                await DisplayAlert(AppResources.AnErrorHasOccurred, string.Format(AppResources.ValidationFieldRequired, AppResources.MasterPassword), AppResources.Ok);
                return;
            }

            var key = _cryptoService.MakeKeyFromPassword(PasswordCell.Entry.Text, _authService.Email);
            if(key.SequenceEqual(_cryptoService.Key))
            {
                _settings.AddOrUpdateValue(Constants.Locked, false);
                await Navigation.PopModalAsync();
            }
            else
            {
                // TODO: keep track of invalid attempts and logout?

                _userDialogs.Alert("Invalid Master Password. Try again.");
                PasswordCell.Entry.Text = string.Empty;
                PasswordCell.Entry.Focus();
            }
        }

        private async Task LogoutAsync()
        {
            if(!await _userDialogs.ConfirmAsync("Are you sure you want to log out?", null, AppResources.Yes, AppResources.Cancel))
            {
                return;
            }

            MessagingCenter.Send(Application.Current, "Logout", (string)null);
        }
    }
}
