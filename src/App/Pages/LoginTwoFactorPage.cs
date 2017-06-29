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
using System.Net;
using FFImageLoading.Forms;

namespace Bit.App.Pages
{
    public class LoginTwoFactorPage : ExtendedContentPage
    {
        private DateTime? _lastAction;
        private IAuthService _authService;
        private IUserDialogs _userDialogs;
        private ISyncService _syncService;
        private IDeviceInfoService _deviceInfoService;
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
            _deviceInfoService = Resolver.Resolve<IDeviceInfoService>();

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

            SubscribeYubiKey(true);
        }

        public FormEntryCell TokenCell { get; set; }
        public ExtendedSwitchCell RememberCell { get; set; }

        private void Init()
        {
            var scrollView = new ScrollView();

            var anotherMethodButton = new ExtendedButton
            {
                Text = "Use another two-step login method",
                Style = (Style)Application.Current.Resources["btn-primaryAccent"],
                Margin = new Thickness(15, 0, 15, 25),
                Command = new Command(() => AnotherMethodAsync()),
                Uppercase = false,
                BackgroundColor = Color.Transparent
            };

            var instruction = new Label
            {
                LineBreakMode = LineBreakMode.WordWrap,
                Margin = new Thickness(15),
                HorizontalTextAlignment = TextAlignment.Center
            };

            RememberCell = new ExtendedSwitchCell
            {
                Text = "Remember me",
                On = false
            };

            if(!_providerType.HasValue)
            {
                instruction.Text = "No providers available.";

                var layout = new StackLayout
                {
                    Children = { instruction, anotherMethodButton },
                    Spacing = 0
                };

                scrollView.Content = layout;

                Title = "Login Unavailable";
                Content = scrollView;
            }
            else if(_providerType.Value == TwoFactorProviderType.Authenticator ||
                _providerType.Value == TwoFactorProviderType.Email)
            {
                var continueToolbarItem = new ToolbarItem(AppResources.Continue, null, async () =>
                {
                    var token = TokenCell?.Entry.Text.Trim().Replace(" ", "");
                    await LogInAsync(token, RememberCell.On);
                }, ToolbarItemOrder.Default, 0);

                var padding = Helpers.OnPlatform(
                    iOS: new Thickness(15, 20),
                    Android: new Thickness(15, 8),
                    WinPhone: new Thickness(15, 20));

                TokenCell = new FormEntryCell(AppResources.VerificationCode, useLabelAsPlaceholder: true,
                    imageSource: "lock", containerPadding: padding);

                TokenCell.Entry.Keyboard = Keyboard.Numeric;
                TokenCell.Entry.ReturnType = ReturnType.Go;

                var table = new TwoFactorTable(
                    new TableSection(" ")
                    {
                        TokenCell,
                        RememberCell
                    });

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
                        break;
                    default:
                        break;
                }

                ToolbarItems.Add(continueToolbarItem);
                Title = AppResources.VerificationCode;

                Content = scrollView;
            }
            else if(_providerType == TwoFactorProviderType.Duo)
            {
                var duoParams = _providers[TwoFactorProviderType.Duo];

                var host = WebUtility.UrlEncode(duoParams["Host"].ToString());
                var req = WebUtility.UrlEncode(duoParams["Signature"].ToString());

                var webView = new HybridWebView
                {
                    Uri = $"http://192.168.1.6:4001/duo-mobile.html?host={host}&request={req}",
                    HorizontalOptions = LayoutOptions.FillAndExpand,
                    VerticalOptions = LayoutOptions.FillAndExpand
                };
                webView.RegisterAction(async (sig) =>
                {
                    await LogInAsync(sig, false);
                });

                Title = "Duo";
                Content = webView;
            }
            else if(_providerType == TwoFactorProviderType.YubiKey)
            {
                instruction.Text = "Hold your YubiKey NEO against the back of the device to continue.";

                var image = new CachedImage
                {
                    Source = "yubikey",
                    VerticalOptions = LayoutOptions.Start,
                    HorizontalOptions = LayoutOptions.Center,
                    WidthRequest = 266,
                    HeightRequest = 160,
                    Margin = new Thickness(0, 0, 0, 25)
                };

                var table = new TwoFactorTable(
                    new TableSection(" ")
                    {
                        RememberCell
                    });

                var layout = new StackLayout
                {
                    Children = { instruction, image, table, anotherMethodButton },
                    Spacing = 0
                };

                scrollView.Content = layout;

                Title = "YubiKey";
                Content = scrollView;
            }
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            ListenYubiKey(true);

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
            ListenYubiKey(false);

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
            await LogInAsync(token, RememberCell.On);
        }

        private async Task LogInAsync(string token, bool remember)
        {
            if(_lastAction.LastActionWasRecent())
            {
                return;
            }
            _lastAction = DateTime.UtcNow;

            if(string.IsNullOrWhiteSpace(token))
            {
                await DisplayAlert(AppResources.AnErrorHasOccurred, string.Format(AppResources.ValidationFieldRequired,
                    AppResources.VerificationCode), AppResources.Ok);
                return;
            }

            _userDialogs.ShowLoading(AppResources.ValidatingCode, MaskType.Black);
            var response = await _authService.TokenPostTwoFactorAsync(_providerType.Value, token, remember,
                _email, _masterPasswordHash, _key);
            _userDialogs.HideLoading();
            if(!response.Success)
            {
                ListenYubiKey(true);
                await DisplayAlert(AppResources.AnErrorHasOccurred, response.ErrorMessage, AppResources.Ok);
                return;
            }

            _googleAnalyticsService.TrackAppEvent("LoggedIn From Two-step");

            if(Device.RuntimePlatform == Device.Android)
            {
                _pushNotification.Register();
            }

            var task = Task.Run(async () => await _syncService.FullSyncAsync(true));

            Device.BeginInvokeOnMainThread(() =>
            {
                Application.Current.MainPage = new MainPage();
            });
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
                            if(provider == TwoFactorProviderType.Duo || provider == TwoFactorProviderType.YubiKey)
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
                            if(provider == TwoFactorProviderType.YubiKey)
                            {
                                continue;
                            }
                            break;
                        case TwoFactorProviderType.YubiKey:
                            if(!_deviceInfoService.NfcEnabled)
                            {
                                continue;
                            }
                            break;
                        default:
                            continue;
                    }

                    provider = p.Key;
                }
            }

            return provider;
        }

        private void ListenYubiKey(bool listen)
        {
            if(_providerType == TwoFactorProviderType.YubiKey)
            {
                MessagingCenter.Send(Application.Current, "ListenYubiKeyOTP", listen);
            }
        }

        private void SubscribeYubiKey(bool subscribe)
        {
            if(_providerType != TwoFactorProviderType.YubiKey)
            {
                return;
            }

            MessagingCenter.Unsubscribe<Application, string>(Application.Current, "GotYubiKeyOTP");
            MessagingCenter.Unsubscribe<Application>(Application.Current, "ResumeYubiKey");
            if(!subscribe)
            {
                return;
            }

            MessagingCenter.Subscribe<Application, string>(Application.Current, "GotYubiKeyOTP", async (sender, otp) =>
            {
                MessagingCenter.Unsubscribe<Application, string>(Application.Current, "GotYubiKeyOTP");
                if(_providerType == TwoFactorProviderType.YubiKey)
                {
                    await LogInAsync(otp, RememberCell.On);
                }
            });

            SubscribeYubiKeyResume();
        }

        private void SubscribeYubiKeyResume()
        {
            MessagingCenter.Subscribe<Application>(Application.Current, "ResumeYubiKey", (sender) =>
            {
                MessagingCenter.Unsubscribe<Application>(Application.Current, "ResumeYubiKey");
                if(_providerType == TwoFactorProviderType.YubiKey)
                {
                    MessagingCenter.Send(Application.Current, "ListenYubiKeyOTP", true);
                    SubscribeYubiKeyResume();
                }
            });
        }

        public class TwoFactorTable : ExtendedTableView
        {
            public TwoFactorTable(TableSection section)
            {
                Intent = TableIntent.Settings;
                EnableScrolling = false;
                HasUnevenRows = true;
                EnableSelection = true;
                NoFooter = true;
                NoHeader = true;
                VerticalOptions = LayoutOptions.Start;
                Root = Root = new TableRoot
                {
                    section
                };

                if(Device.RuntimePlatform == Device.iOS)
                {
                    RowHeight = -1;
                    EstimatedRowHeight = 70;
                }
            }
        }
    }
}
