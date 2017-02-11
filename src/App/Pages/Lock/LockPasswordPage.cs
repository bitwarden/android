using System;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Xamarin.Forms;
using XLabs.Ioc;
using Bit.App.Controls;
using System.Linq;
using Plugin.Settings.Abstractions;

namespace Bit.App.Pages
{
    public class LockPasswordPage : BaseLockPage
    {
        private readonly IAuthService _authService;
        private readonly ISettings _settings;
        private readonly ICryptoService _cryptoService;

        public LockPasswordPage()
        {
            _authService = Resolver.Resolve<IAuthService>();
            _settings = Resolver.Resolve<ISettings>();
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

            PasswordCell = new FormEntryCell(AppResources.MasterPassword, isPassword: true,
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

            var logoutButton = new ExtendedButton
            {
                Text = AppResources.LogOut,
                Command = new Command(async () => await LogoutAsync()),
                VerticalOptions = LayoutOptions.End,
                Style = (Style)Application.Current.Resources["btn-primaryAccent"],
                BackgroundColor = Color.Transparent,
                Uppercase = false
            };

            var stackLayout = new StackLayout
            {
                Spacing = 10,
                Children = { table, logoutButton }
            };

            var scrollView = new ScrollView { Content = stackLayout };

            if(Device.OS == TargetPlatform.iOS)
            {
                table.RowHeight = -1;
                table.EstimatedRowHeight = 70;
            }

            var loginToolbarItem = new ToolbarItem(AppResources.Submit, null, async () =>
            {
                await CheckPasswordAsync();
            }, ToolbarItemOrder.Default, 0);

            ToolbarItems.Add(loginToolbarItem);
            Title = AppResources.VerifyMasterPassword;
            Content = scrollView;
        }

        private void Entry_Completed(object sender, EventArgs e)
        {
            var task = CheckPasswordAsync();
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            PasswordCell.Entry.FocusWithDelay();
        }

        protected async Task CheckPasswordAsync()
        {
            if(string.IsNullOrWhiteSpace(PasswordCell.Entry.Text))
            {
                await DisplayAlert(AppResources.AnErrorHasOccurred, string.Format(AppResources.ValidationFieldRequired,
                    AppResources.MasterPassword), AppResources.Ok);
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

                UserDialogs.Alert(AppResources.InvalidMasterPassword);
                PasswordCell.Entry.Text = string.Empty;
                PasswordCell.Entry.Focus();
            }
        }
    }
}
