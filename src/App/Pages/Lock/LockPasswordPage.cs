using System;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Resources;
using Xamarin.Forms;
using XLabs.Ioc;
using Bit.App.Controls;
using System.Linq;
using Bit.App.Utilities;

namespace Bit.App.Pages
{
    public class LockPasswordPage : BaseLockPage
    {
        private readonly IAuthService _authService;
        private readonly IAppSettingsService _appSettingsService;
        private readonly ICryptoService _cryptoService;
        private DateTime? _lastAction;

        public LockPasswordPage()
        {
            _authService = Resolver.Resolve<IAuthService>();
            _appSettingsService = Resolver.Resolve<IAppSettingsService>();
            _cryptoService = Resolver.Resolve<ICryptoService>();

            Init();
        }

        public FormEntryCell PasswordCell { get; set; }

        public void Init()
        {
            var padding = Helpers.OnPlatform(
                iOS: new Thickness(15, 20),
                Android: new Thickness(15, 8),
                Windows: new Thickness(10, 8));

            PasswordCell = new FormEntryCell(AppResources.MasterPassword, isPassword: true,
                useLabelAsPlaceholder: true, imageSource: "lock.png", containerPadding: padding);

            PasswordCell.Entry.ReturnType = Enums.ReturnType.Go;

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
                    new TableSection(Helpers.GetEmptyTableSectionTitle())
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

            if(Device.RuntimePlatform == Device.iOS)
            {
                table.RowHeight = -1;
                table.EstimatedRowHeight = 70;
            }

            var loginToolbarItem = new ToolbarItem(AppResources.Submit, Helpers.ToolbarImage("login.png"), async () =>
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
            PasswordCell.InitEvents();
            PasswordCell.Entry.Completed += Entry_Completed;

            if(Device.RuntimePlatform == Device.Android)
            {
                Task.Run(async () =>
                {
                    for(int i = 0; i < 5; i++)
                    {
                        if(!PasswordCell.Entry.IsFocused)
                        {
                            Device.BeginInvokeOnMainThread(() => PasswordCell.Entry.FocusWithDelay());
                        }
                        else
                        {
                            break;
                        }

                        await Task.Delay(1000);
                    }
                });
            }
            else
            {
                PasswordCell.Entry.Focus();
            }
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            PasswordCell.Dispose();
            PasswordCell.Entry.Completed -= Entry_Completed;
        }

        protected async Task CheckPasswordAsync()
        {
            if(_lastAction.LastActionWasRecent())
            {
                return;
            }
            _lastAction = DateTime.UtcNow;

            if(string.IsNullOrWhiteSpace(PasswordCell.Entry.Text))
            {
                await DisplayAlert(AppResources.AnErrorHasOccurred, string.Format(AppResources.ValidationFieldRequired,
                    AppResources.MasterPassword), AppResources.Ok);
                return;
            }

            var key = _cryptoService.MakeKeyFromPassword(PasswordCell.Entry.Text, _authService.Email);
            if(key.Key.SequenceEqual(_cryptoService.Key.Key))
            {
                _appSettingsService.Locked = false;
                await Navigation.PopModalAsync();
            }
            else
            {
                // TODO: keep track of invalid attempts and logout?

                await DisplayAlert(null, AppResources.InvalidMasterPassword, AppResources.Ok);
                PasswordCell.Entry.Text = string.Empty;
                PasswordCell.Entry.Focus();
            }
        }
    }
}
