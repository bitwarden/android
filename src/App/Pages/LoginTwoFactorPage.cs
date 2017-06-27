using System;
using Bit.App.Abstractions;
using Bit.App.Controls;
using Bit.App.Resources;
using Xamarin.Forms;
using XLabs.Ioc;
using Acr.UserDialogs;
using System.Threading.Tasks;
using PushNotification.Plugin.Abstractions;
using Bit.App.Models;
using Bit.App.Utilities;
using Bit.App.Enums;
using System.Collections.Generic;
using System.Linq;

namespace Bit.App.Pages
{
    public class LoginTwoFactorPage : ExtendedContentPage
    {
        private IAuthService _authService;
        private IUserDialogs _userDialogs;
        private ISyncService _syncService;
        private IGoogleAnalyticsService _googleAnalyticsService;
        private IPushNotification _pushNotification;
        private readonly string _email;
        private readonly string _masterPasswordHash;
        private readonly SymmetricCryptoKey _key;
        private readonly Dictionary<TwoFactorProviderType, Dictionary<string, object>> _providers;
        private readonly TwoFactorProviderType? _providerType;
        private readonly FullLoginResult _result;

        public LoginTwoFactorPage(string email, FullLoginResult result, TwoFactorProviderType? type = null)
            : base(updateActivity: false)
        {
            _email = email;
            _result = result;
            _masterPasswordHash = result.MasterPasswordHash;
            _key = result.Key;
            _providers = result.TwoFactorProviders;
            _providerType = type ?? GetDefaultProvider();

            _authService = Resolver.Resolve<IAuthService>();
            _userDialogs = Resolver.Resolve<IUserDialogs>();
            _syncService = Resolver.Resolve<ISyncService>();
            _googleAnalyticsService = Resolver.Resolve<IGoogleAnalyticsService>();
            _pushNotification = Resolver.Resolve<IPushNotification>();

            Init();
        }

        public FormEntryCell TokenCell { get; set; }
        public ExtendedSwitchCell RememberCell { get; set; }

        private void Init()
        {
            var scrollView = new ScrollView();

            var continueToolbarItem = new ToolbarItem(AppResources.Continue, null, async () =>
            {
                var token = TokenCell?.Entry.Text.Trim().Replace(" ", "");
                await LogInAsync(token);
            }, ToolbarItemOrder.Default, 0);

            if(!_providerType.HasValue)
            {
                var noProviderLabel = new Label
                {
                    Text = "No provider.",
                    LineBreakMode = LineBreakMode.WordWrap,
                    Margin = new Thickness(15),
                    HorizontalTextAlignment = TextAlignment.Center
                };
                scrollView.Content = noProviderLabel;
            }
            else
            {
                var padding = Helpers.OnPlatform(
                    iOS: new Thickness(15, 20),
                    Android: new Thickness(15, 8),
                    WinPhone: new Thickness(15, 20));

                TokenCell = new FormEntryCell(AppResources.VerificationCode, useLabelAsPlaceholder: true,
                    imageSource: "lock", containerPadding: padding);

                TokenCell.Entry.Keyboard = Keyboard.Numeric;
                TokenCell.Entry.ReturnType = ReturnType.Go;

                RememberCell = new ExtendedSwitchCell
                {
                    Text = "Remember me",
                    On = false
                };

                var table = new ExtendedTableView
                {
                    Intent = TableIntent.Settings,
                    EnableScrolling = false,
                    HasUnevenRows = true,
                    EnableSelection = true,
                    NoFooter = true,
                    NoHeader = true,
                    VerticalOptions = LayoutOptions.Start,
                    Root = new TableRoot
                    {
                        new TableSection(" ")
                        {
                            TokenCell,
                            RememberCell
                        }
                    }
                };


                if(Device.RuntimePlatform == Device.iOS)
                {
                    table.RowHeight = -1;
                    table.EstimatedRowHeight = 70;
                }

                var instruction = new Label
                {
                    Text = AppResources.EnterVerificationCode,
                    LineBreakMode = LineBreakMode.WordWrap,
                    Margin = new Thickness(15),
                    HorizontalTextAlignment = TextAlignment.Center
                };

                var anotherMethodButton = new ExtendedButton
                {
                    Text = "Use another two-step login method",
                    Style = (Style)Application.Current.Resources["btn-primaryAccent"],
                    Margin = new Thickness(15, 0, 15, 25),
                    Command = new Command(() => AnotherMethodAsync()),
                    Uppercase = false,
                    BackgroundColor = Color.Transparent
                };

                var layout = new StackLayout
                {
                    Children = { instruction, table, anotherMethodButton },
                    Spacing = 0
                };

                scrollView.Content = layout;

                switch(_providerType.Value)
                {
                    case TwoFactorProviderType.Authenticator:
                        instruction.Text = "Enter the 6 digit verification code from your authenticator app.";
                        layout.Children.Add(instruction);
                        layout.Children.Add(table);
                        layout.Children.Add(anotherMethodButton);

                        ToolbarItems.Add(continueToolbarItem);
                        Title = AppResources.VerificationCode;
                        break;
                    case TwoFactorProviderType.Email:
                        var emailParams = _providers[TwoFactorProviderType.Email];
                        var redactedEmail = emailParams["Email"].ToString();

                        instruction.Text = "Enter the 6 digit verification code from your authenticator app.";
                        var resendEmailButton = new ExtendedButton
                        {
                            Text = $"Enter the 6 digit verification code that was emailed to {redactedEmail}.",
                            Style = (Style)Application.Current.Resources["btn-primaryAccent"],
                            Margin = new Thickness(15, 0, 15, 25),
                            Command = new Command(() => SendEmail()),
                            Uppercase = false,
                            BackgroundColor = Color.Transparent
                        };

                        layout.Children.Add(instruction);
                        layout.Children.Add(table);
                        layout.Children.Add(resendEmailButton);
                        layout.Children.Add(anotherMethodButton);

                        ToolbarItems.Add(continueToolbarItem);
                        Title = AppResources.VerificationCode;
                        break;
                    case TwoFactorProviderType.Duo:
                        break;
                    default:
                        break;
                }
            }

            Content = scrollView;
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();

            if(TokenCell != null)
            {
                TokenCell.InitEvents();
                TokenCell.Entry.FocusWithDelay();
                TokenCell.Entry.Completed += Entry_Completed;
            }
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();

            if(TokenCell != null)
            {
                TokenCell.Dispose();
                TokenCell.Entry.Completed -= Entry_Completed;
            }
        }

        private async void AnotherMethodAsync()
        {
            await Navigation.PushForDeviceAsync(new TwoFactorMethodsPage(_email, _result));
        }

        private void SendEmail()
        {

        }

        private void Recover()
        {
            Device.OpenUri(new Uri("https://help.bitwarden.com/article/lost-two-step-device/"));
        }

        private async void Entry_Completed(object sender, EventArgs e)
        {
            var token = TokenCell.Entry.Text.Trim().Replace(" ", "");
            await LogInAsync(token);
        }

        private async Task LogInAsync(string token)
        {
            if(string.IsNullOrWhiteSpace(token))
            {
                await DisplayAlert(AppResources.AnErrorHasOccurred, string.Format(AppResources.ValidationFieldRequired,
                    AppResources.VerificationCode), AppResources.Ok);
                return;
            }

            _userDialogs.ShowLoading(AppResources.ValidatingCode, MaskType.Black);
            var response = await _authService.TokenPostTwoFactorAsync(_providerType.Value, token, RememberCell.On,
                _email, _masterPasswordHash, _key);
            _userDialogs.HideLoading();
            if(!response.Success)
            {
                await DisplayAlert(AppResources.AnErrorHasOccurred, response.ErrorMessage, AppResources.Ok);
                return;
            }

            _googleAnalyticsService.TrackAppEvent("LoggedIn From Two-step");

            if(Device.RuntimePlatform == Device.Android)
            {
                _pushNotification.Register();
            }

            var task = Task.Run(async () => await _syncService.FullSyncAsync(true));
            Application.Current.MainPage = new MainPage();
        }

        private TwoFactorProviderType? GetDefaultProvider()
        {
            TwoFactorProviderType? provider = null;

            if(_providers != null)
            {
                if(_providers.Count == 1)
                {
                    return _providers.First().Key;
                }

                foreach(var p in _providers)
                {
                    switch(p.Key)
                    {
                        case TwoFactorProviderType.Authenticator:
                            if(provider == TwoFactorProviderType.Duo)
                            {
                                continue;
                            }
                            break;
                        case TwoFactorProviderType.Email:
                            if(provider.HasValue)
                            {
                                continue;
                            }
                            break;
                        case TwoFactorProviderType.Duo:
                            break;
                        default:
                            continue;
                    }

                    provider = p.Key;
                }
            }

            return provider;
        }
    }
}
