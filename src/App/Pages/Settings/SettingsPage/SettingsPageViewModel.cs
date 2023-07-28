using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Pages.Accounts;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Models.Domain;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Xamarin.CommunityToolkit.ObjectModel;
using Microsoft.Maui.Controls;
using Microsoft.Maui;

namespace Bit.App.Pages
{
    public class SettingsPageViewModel : BaseViewModel
    {
        private readonly IPlatformUtilsService _platformUtilsService;
        private readonly ICryptoService _cryptoService;
        private readonly IStateService _stateService;
        private readonly IDeviceActionService _deviceActionService;
        private readonly IAutofillHandler _autofillHandler;
        private readonly IEnvironmentService _environmentService;
        private readonly IMessagingService _messagingService;
        private readonly IVaultTimeoutService _vaultTimeoutService;
        private readonly ISyncService _syncService;
        private readonly IBiometricService _biometricService;
        private readonly IPolicyService _policyService;
        private readonly ILocalizeService _localizeService;
        private readonly IKeyConnectorService _keyConnectorService;
        private readonly IClipboardService _clipboardService;
        private readonly ILogger _loggerService;
        private readonly IPushNotificationService _pushNotificationService;
        private readonly IAuthService _authService;
        private readonly IWatchDeviceService _watchDeviceService;
        private const int CustomVaultTimeoutValue = -100;

        private bool _supportsBiometric;
        private bool _pin;
        private bool _biometric;
        private bool _screenCaptureAllowed;
        private string _lastSyncDate;
        private string _vaultTimeoutDisplayValue;
        private string _vaultTimeoutActionDisplayValue;
        private bool _showChangeMasterPassword;
        private bool _reportLoggingEnabled;
        private bool _approvePasswordlessLoginRequests;
        private bool _shouldConnectToWatch;
        private readonly static List<KeyValuePair<string, int?>> VaultTimeoutOptions =
            new List<KeyValuePair<string, int?>>
            {
                new KeyValuePair<string, int?>(AppResources.Immediately, 0),
                new KeyValuePair<string, int?>(AppResources.OneMinute, 1),
                new KeyValuePair<string, int?>(AppResources.FiveMinutes, 5),
                new KeyValuePair<string, int?>(AppResources.FifteenMinutes, 15),
                new KeyValuePair<string, int?>(AppResources.ThirtyMinutes, 30),
                new KeyValuePair<string, int?>(AppResources.OneHour, 60),
                new KeyValuePair<string, int?>(AppResources.FourHours, 240),
                new KeyValuePair<string, int?>(AppResources.OnRestart, -1),
                new KeyValuePair<string, int?>(AppResources.Never, null),
                new KeyValuePair<string, int?>(AppResources.Custom, CustomVaultTimeoutValue),
            };
        private readonly static List<KeyValuePair<string, VaultTimeoutAction>> VaultTimeoutActionOptions =
            new List<KeyValuePair<string, VaultTimeoutAction>>
            {
                new KeyValuePair<string, VaultTimeoutAction>(AppResources.Lock, VaultTimeoutAction.Lock),
                new KeyValuePair<string, VaultTimeoutAction>(AppResources.LogOut, VaultTimeoutAction.Logout),
            };

        private Policy _vaultTimeoutPolicy;
        private int? _vaultTimeout;
        private List<KeyValuePair<string, int?>> _vaultTimeoutOptions = VaultTimeoutOptions;
        private List<KeyValuePair<string, VaultTimeoutAction>> _vaultTimeoutActionOptions = VaultTimeoutActionOptions;

        public SettingsPageViewModel()
        {
            _platformUtilsService = ServiceContainer.Resolve<IPlatformUtilsService>("platformUtilsService");
            _cryptoService = ServiceContainer.Resolve<ICryptoService>("cryptoService");
            _stateService = ServiceContainer.Resolve<IStateService>("stateService");
            _deviceActionService = ServiceContainer.Resolve<IDeviceActionService>("deviceActionService");
            _autofillHandler = ServiceContainer.Resolve<IAutofillHandler>();
            _environmentService = ServiceContainer.Resolve<IEnvironmentService>("environmentService");
            _messagingService = ServiceContainer.Resolve<IMessagingService>("messagingService");
            _vaultTimeoutService = ServiceContainer.Resolve<IVaultTimeoutService>("vaultTimeoutService");
            _syncService = ServiceContainer.Resolve<ISyncService>("syncService");
            _biometricService = ServiceContainer.Resolve<IBiometricService>("biometricService");
            _policyService = ServiceContainer.Resolve<IPolicyService>("policyService");
            _localizeService = ServiceContainer.Resolve<ILocalizeService>("localizeService");
            _keyConnectorService = ServiceContainer.Resolve<IKeyConnectorService>("keyConnectorService");
            _clipboardService = ServiceContainer.Resolve<IClipboardService>("clipboardService");
            _loggerService = ServiceContainer.Resolve<ILogger>("logger");
            _pushNotificationService = ServiceContainer.Resolve<IPushNotificationService>();
            _authService = ServiceContainer.Resolve<IAuthService>();
            _watchDeviceService = ServiceContainer.Resolve<IWatchDeviceService>();
            GroupedItems = new ObservableRangeCollection<ISettingsPageListItem>();
            PageTitle = AppResources.Settings;

            ExecuteSettingItemCommand = new AsyncCommand<SettingsPageListItem>(item => item.ExecuteAsync(), onException: _loggerService.Exception, allowsMultipleExecutions: false);
        }

        public ObservableRangeCollection<ISettingsPageListItem> GroupedItems { get; set; }

        public IAsyncCommand<SettingsPageListItem> ExecuteSettingItemCommand { get; }

        public async Task InitAsync()
        {
            _supportsBiometric = await _platformUtilsService.SupportsBiometricAsync();
            var lastSync = await _syncService.GetLastSyncAsync();
            if (lastSync != null)
            {
                lastSync = lastSync.Value.ToLocalTime();
                _lastSyncDate = string.Format("{0} {1}",
                    _localizeService.GetLocaleShortDate(lastSync.Value),
                    _localizeService.GetLocaleShortTime(lastSync.Value));
            }

            _vaultTimeoutPolicy = null;
            _vaultTimeoutOptions = VaultTimeoutOptions;
            _vaultTimeoutActionOptions = VaultTimeoutActionOptions;

            _vaultTimeout = await _vaultTimeoutService.GetVaultTimeout();
            _vaultTimeoutDisplayValue = _vaultTimeoutOptions.FirstOrDefault(o => o.Value == _vaultTimeout).Key;
            _vaultTimeoutDisplayValue ??= _vaultTimeoutOptions.Where(o => o.Value == CustomVaultTimeoutValue).First().Key;

            var action = await _vaultTimeoutService.GetVaultTimeoutAction() ?? VaultTimeoutAction.Lock;
            _vaultTimeoutActionDisplayValue = _vaultTimeoutActionOptions.FirstOrDefault(o => o.Value == action).Key;

            if (await _policyService.PolicyAppliesToUser(PolicyType.MaximumVaultTimeout))
            {
                // if we have a vault timeout policy, we need to filter the timeout options
                _vaultTimeoutPolicy = (await _policyService.GetAll(PolicyType.MaximumVaultTimeout)).First();
                var policyMinutes = _vaultTimeoutPolicy.GetInt(Policy.MINUTES_KEY);
                _vaultTimeoutOptions = _vaultTimeoutOptions.Where(t =>
                    t.Value <= policyMinutes &&
                    (t.Value > 0 || t.Value == CustomVaultTimeoutValue) &&
                    t.Value != null).ToList();
            }

            var pinSet = await _vaultTimeoutService.IsPinLockSetAsync();
            _pin = pinSet.Item1 || pinSet.Item2;
            _biometric = await _vaultTimeoutService.IsBiometricLockSetAsync();
            _screenCaptureAllowed = await _stateService.GetScreenCaptureAllowedAsync();

            if (_vaultTimeoutDisplayValue == null)
            {
                _vaultTimeoutDisplayValue = AppResources.Custom;
            }

            _showChangeMasterPassword = IncludeLinksWithSubscriptionInfo() &&
                !await _keyConnectorService.GetUsesKeyConnector();
            _reportLoggingEnabled = await _loggerService.IsEnabled();
            _approvePasswordlessLoginRequests = await _stateService.GetApprovePasswordlessLoginsAsync();
            _shouldConnectToWatch = await _stateService.GetShouldConnectToWatchAsync();

            BuildList();
        }

        public async Task AboutAsync()
        {
            var debugText = string.Format("{0}: {1} ({2})", AppResources.Version,
                _platformUtilsService.GetApplicationVersion(), _deviceActionService.GetBuildNumber());

#if DEBUG
            var pushNotificationsRegistered = ServiceContainer.Resolve<IPushNotificationService>("pushNotificationService").IsRegisteredForPush;
            var pnServerRegDate = await _stateService.GetPushLastRegistrationDateAsync();
            var pnServerError = await _stateService.GetPushInstallationRegistrationErrorAsync();

            var pnServerRegDateMessage = default(DateTime) == pnServerRegDate ? "-" : $"{pnServerRegDate.GetValueOrDefault().ToShortDateString()}-{pnServerRegDate.GetValueOrDefault().ToShortTimeString()} UTC";
            var errorMessage = string.IsNullOrEmpty(pnServerError) ? string.Empty : $"Push Notifications Server Registration error: {pnServerError}";

            var text = string.Format("© Bitwarden Inc. 2015-{0}\n\n{1}\nPush Notifications registered:{2}\nPush Notifications Server Last Date :{3}\n{4}", DateTime.Now.Year, debugText, pushNotificationsRegistered, pnServerRegDateMessage, errorMessage);
#else
            var text = string.Format("© Bitwarden Inc. 2015-{0}\n\n{1}", DateTime.Now.Year, debugText);
#endif

            var copy = await _platformUtilsService.ShowDialogAsync(text, AppResources.Bitwarden, AppResources.Copy,
                AppResources.Close);
            if (copy)
            {
                await _clipboardService.CopyTextAsync(debugText);
            }
        }

        public void Help()
        {
            _platformUtilsService.LaunchUri("https://bitwarden.com/help/");
        }

        public async Task FingerprintAsync()
        {
            List<string> fingerprint;
            try
            {
                fingerprint = await _cryptoService.GetFingerprintAsync(await _stateService.GetActiveUserIdAsync());
            }
            catch (Exception e) when (e.Message == "No public key available.")
            {
                return;
            }
            var phrase = string.Join("-", fingerprint);
            var text = string.Format("{0}:\n\n{1}", AppResources.YourAccountsFingerprint, phrase);
            var learnMore = await _platformUtilsService.ShowDialogAsync(text, AppResources.FingerprintPhrase,
                AppResources.LearnMore, AppResources.Close);
            if (learnMore)
            {
                _platformUtilsService.LaunchUri("https://bitwarden.com/help/fingerprint-phrase/");
            }
        }

        public void Rate()
        {
            _deviceActionService.RateApp();
        }

        public void Import()
        {
            _platformUtilsService.LaunchUri("https://bitwarden.com/help/import-data/");
        }

        public void WebVault()
        {
            _platformUtilsService.LaunchUri(_environmentService.GetWebVaultUrl());
        }

        public async Task ShareAsync()
        {
            var confirmed = await _platformUtilsService.ShowDialogAsync(AppResources.LearnOrgConfirmation,
               AppResources.LearnOrg, AppResources.Yes, AppResources.Cancel);
            if (confirmed)
            {
                _platformUtilsService.LaunchUri("https://bitwarden.com/help/about-organizations/");
            }
        }

        public async Task TwoStepAsync()
        {
            var confirmed = await _platformUtilsService.ShowDialogAsync(AppResources.TwoStepLoginConfirmation,
                AppResources.TwoStepLogin, AppResources.Yes, AppResources.Cancel);
            if (confirmed)
            {
                _platformUtilsService.LaunchUri($"{_environmentService.GetWebVaultUrl()}/#/settings");
            }
        }

        public async Task ChangePasswordAsync()
        {
            var confirmed = await _platformUtilsService.ShowDialogAsync(AppResources.ChangePasswordConfirmation,
                AppResources.ChangeMasterPassword, AppResources.Yes, AppResources.Cancel);
            if (confirmed)
            {
                _platformUtilsService.LaunchUri($"{_environmentService.GetWebVaultUrl()}/#/settings");
            }
        }

        public async Task LogOutAsync()
        {
            var confirmed = await _platformUtilsService.ShowDialogAsync(AppResources.LogoutConfirmation,
                AppResources.LogOut, AppResources.Yes, AppResources.Cancel);
            if (confirmed)
            {
                _messagingService.Send("logout");
            }
        }

        public async Task LockAsync()
        {
            await _vaultTimeoutService.LockAsync(true, true);
        }

        public async Task VaultTimeoutAsync(bool promptOptions = true, int? newTimeout = 0)
        {
            var oldTimeout = _vaultTimeout;

            var options = _vaultTimeoutOptions.Select(
                o => o.Key == _vaultTimeoutDisplayValue ? $"✓ {o.Key}" : o.Key).ToArray();
            if (promptOptions)
            {
                var selection = await Page.DisplayActionSheet(AppResources.VaultTimeout,
                    AppResources.Cancel, null, options);
                if (selection == null || selection == AppResources.Cancel)
                {
                    return;
                }
                var cleanSelection = selection.Replace("✓ ", string.Empty);
                var selectionOption = _vaultTimeoutOptions.FirstOrDefault(o => o.Key == cleanSelection);

                // Check if the selected Timeout action is "Never" and if it's different from the previous selected value
                if (selectionOption.Value == null && selectionOption.Value != oldTimeout)
                {
                    var confirmed = await _platformUtilsService.ShowDialogAsync(AppResources.NeverLockWarning,
                        AppResources.Warning, AppResources.Yes, AppResources.Cancel);
                    if (!confirmed)
                    {
                        return;
                    }
                }
                _vaultTimeoutDisplayValue = selectionOption.Key;
                newTimeout = selectionOption.Value;
            }

            if (_vaultTimeoutPolicy != null)
            {
                var maximumTimeout = _vaultTimeoutPolicy.GetInt(Policy.MINUTES_KEY);

                if (newTimeout > maximumTimeout)
                {
                    await _platformUtilsService.ShowDialogAsync(AppResources.VaultTimeoutToLarge, AppResources.Warning);
                    var timeout = await _vaultTimeoutService.GetVaultTimeout();
                    _vaultTimeoutDisplayValue = _vaultTimeoutOptions.FirstOrDefault(o => o.Value == timeout).Key ??
                                                AppResources.Custom;
                    return;
                }
            }

            await _vaultTimeoutService.SetVaultTimeoutOptionsAsync(newTimeout,
                GetVaultTimeoutActionFromKey(_vaultTimeoutActionDisplayValue));

            if (newTimeout != CustomVaultTimeoutValue)
            {
                _vaultTimeout = newTimeout;
            }
            if (oldTimeout != newTimeout)
            {
                await Device.InvokeOnMainThreadAsync(BuildList);
            }
        }

        public async Task LoggerReportingAsync()
        {
            var options = new[]
            {
                    CreateSelectableOption(AppResources.Yes, _reportLoggingEnabled),
                    CreateSelectableOption(AppResources.No, !_reportLoggingEnabled),
            };

            var selection = await Page.DisplayActionSheet(AppResources.SubmitCrashLogsDescription, AppResources.Cancel, null, options);

            if (selection == null || selection == AppResources.Cancel)
            {
                return;
            }

            await _loggerService.SetEnabled(CompareSelection(selection, AppResources.Yes));
            _reportLoggingEnabled = await _loggerService.IsEnabled();
            BuildList();
        }

        public async Task ApproveLoginRequestsAsync()
        {
            var options = new[]
            {
                    CreateSelectableOption(AppResources.Yes, _approvePasswordlessLoginRequests),
                    CreateSelectableOption(AppResources.No, !_approvePasswordlessLoginRequests),
            };

            var selection = await Page.DisplayActionSheet(AppResources.UseThisDeviceToApproveLoginRequestsMadeFromOtherDevices, AppResources.Cancel, null, options);

            if (selection == null || selection == AppResources.Cancel)
            {
                return;
            }

            _approvePasswordlessLoginRequests = CompareSelection(selection, AppResources.Yes);
            await _stateService.SetApprovePasswordlessLoginsAsync(_approvePasswordlessLoginRequests);

            BuildList();

            if (!_approvePasswordlessLoginRequests || await _pushNotificationService.AreNotificationsSettingsEnabledAsync())
            {
                return;
            }

            var openAppSettingsResult = await _platformUtilsService.ShowDialogAsync(AppResources.ReceivePushNotificationsForNewLoginRequests, title: string.Empty, confirmText: AppResources.Settings, cancelText: AppResources.NoThanks);
            if (openAppSettingsResult)
            {
                _deviceActionService.OpenAppSettings();
            }
        }

        public async Task VaultTimeoutActionAsync()
        {
            if (_vaultTimeoutPolicy != null &&
                !string.IsNullOrEmpty(_vaultTimeoutPolicy.GetString(Policy.ACTION_KEY)))
            {
                // do nothing if we have a policy set
                return;
            }
            var options = _vaultTimeoutActionOptions.Select(o =>
                o.Key == _vaultTimeoutActionDisplayValue ? $"✓ {o.Key}" : o.Key).ToArray();
            var selection = await Page.DisplayActionSheet(AppResources.VaultTimeoutAction,
                AppResources.Cancel, null, options);
            if (selection == null || selection == AppResources.Cancel)
            {
                return;
            }
            var cleanSelection = selection.Replace("✓ ", string.Empty);
            if (cleanSelection == AppResources.LogOut)
            {
                var confirmed = await _platformUtilsService.ShowDialogAsync(AppResources.VaultTimeoutLogOutConfirmation,
                    AppResources.Warning, AppResources.Yes, AppResources.Cancel);
                if (!confirmed)
                {
                    // Reset to lock and continue process as if lock were selected
                    cleanSelection = AppResources.Lock;
                }
            }
            var selectionOption = _vaultTimeoutActionOptions.FirstOrDefault(o => o.Key == cleanSelection);
            var changed = _vaultTimeoutActionDisplayValue != selectionOption.Key;
            _vaultTimeoutActionDisplayValue = selectionOption.Key;
            await _vaultTimeoutService.SetVaultTimeoutOptionsAsync(_vaultTimeout,
                selectionOption.Value);
            if (changed)
            {
                _messagingService.Send("vaultTimeoutActionChanged");
            }
            BuildList();
        }

        public async Task UpdatePinAsync()
        {
            _pin = !_pin;
            if (_pin)
            {
                var pin = await _deviceActionService.DisplayPromptAync(AppResources.EnterPIN,
                    AppResources.SetPINDescription, null, AppResources.Submit, AppResources.Cancel, true);
                if (!string.IsNullOrWhiteSpace(pin))
                {
                    var masterPassOnRestart = false;
                    if (!await _keyConnectorService.GetUsesKeyConnector())
                    {
                        masterPassOnRestart = await _platformUtilsService.ShowDialogAsync(
                            AppResources.PINRequireMasterPasswordRestart, AppResources.UnlockWithPIN,
                            AppResources.Yes, AppResources.No);
                    }

                    var kdfConfig = await _stateService.GetActiveUserCustomDataAsync(a => new KdfConfig(a?.Profile));
                    var email = await _stateService.GetEmailAsync();
                    var pinKey = await _cryptoService.MakePinKeyAysnc(pin, email, kdfConfig);
                    var key = await _cryptoService.GetKeyAsync();
                    var pinProtectedKey = await _cryptoService.EncryptAsync(key.Key, pinKey);

                    if (masterPassOnRestart)
                    {
                        var encPin = await _cryptoService.EncryptAsync(pin);
                        await _stateService.SetProtectedPinAsync(encPin.EncryptedString);
                        await _stateService.SetPinProtectedKeyAsync(pinProtectedKey);
                    }
                    else
                    {
                        await _stateService.SetPinProtectedAsync(pinProtectedKey.EncryptedString);
                    }
                }
                else
                {
                    _pin = false;
                }
            }
            if (!_pin)
            {
                await _cryptoService.ClearPinProtectedKeyAsync();
                await _vaultTimeoutService.ClearAsync();
            }
            BuildList();
        }

        public async Task UpdateBiometricAsync()
        {
            var current = _biometric;
            if (_biometric)
            {
                _biometric = false;
            }
            else if (await _platformUtilsService.SupportsBiometricAsync())
            {
                // TODO Xamarin.Forms.Device.RuntimePlatform is no longer supported. Use Microsoft.Maui.Devices.DeviceInfo.Platform instead. For more details see https://learn.microsoft.com/en-us/dotnet/maui/migration/forms-projects#device-changes
                _biometric = await _platformUtilsService.AuthenticateBiometricAsync(null,
                    Device.RuntimePlatform == Device.Android ? "." : null);
            }
            if (_biometric == current)
            {
                return;
            }
            if (_biometric)
            {
                await _biometricService.SetupBiometricAsync();
                await _stateService.SetBiometricUnlockAsync(true);
            }
            else
            {
                await _stateService.SetBiometricUnlockAsync(null);
            }
            await _stateService.SetBiometricLockedAsync(false);
            await _cryptoService.ToggleKeyAsync();
            BuildList();
        }

        public void BuildList()
        {
            //TODO: Refactor this once navigation is abstracted so that it doesn't depend on Page, e.g. Page.Navigation.PushModalAsync...

            // TODO Xamarin.Forms.Device.RuntimePlatform is no longer supported. Use Microsoft.Maui.Devices.DeviceInfo.Platform instead. For more details see https://learn.microsoft.com/en-us/dotnet/maui/migration/forms-projects#device-changes
                        var doUpper = Device.RuntimePlatform != Device.Android;
            var autofillItems = new List<SettingsPageListItem>();
            // TODO Xamarin.Forms.Device.RuntimePlatform is no longer supported. Use Microsoft.Maui.Devices.DeviceInfo.Platform instead. For more details see https://learn.microsoft.com/en-us/dotnet/maui/migration/forms-projects#device-changes
            if (Device.RuntimePlatform == Device.Android)
            {
                autofillItems.Add(new SettingsPageListItem
                {
                    Name = AppResources.AutofillServices,
                    SubLabel = _autofillHandler.AutofillServicesEnabled() ? AppResources.On : AppResources.Off,
                    ExecuteAsync = () => Page.Navigation.PushModalAsync(new NavigationPage(new AutofillServicesPage(Page as SettingsPage)))
                });
            }
            else
            {
                if (_deviceActionService.SystemMajorVersion() >= 12)
                {
                    autofillItems.Add(new SettingsPageListItem
                    {
                        Name = AppResources.PasswordAutofill,
                        ExecuteAsync = () => Page.Navigation.PushModalAsync(new NavigationPage(new AutofillPage()))
                    });
                }
                autofillItems.Add(new SettingsPageListItem
                {
                    Name = AppResources.AppExtension,
                    ExecuteAsync = () => Page.Navigation.PushModalAsync(new NavigationPage(new ExtensionPage()))
                });
            }
            var manageItems = new List<SettingsPageListItem>
            {
                new SettingsPageListItem
                {
                    Name = AppResources.Folders,
                    ExecuteAsync = () => Page.Navigation.PushModalAsync(new NavigationPage(new FoldersPage()))
                },
                new SettingsPageListItem
                {
                    Name = AppResources.Sync,
                    SubLabel = _lastSyncDate,
                    ExecuteAsync = () => Page.Navigation.PushModalAsync(new NavigationPage(new SyncPage()))
                }
            };
            var securityItems = new List<SettingsPageListItem>
            {
                new SettingsPageListItem
                {
                    Name = AppResources.VaultTimeout,
                    SubLabel = _vaultTimeoutDisplayValue,
                    ExecuteAsync = () => VaultTimeoutAsync() },
                new SettingsPageListItem
                {
                    Name = AppResources.VaultTimeoutAction,
                    SubLabel = _vaultTimeoutActionDisplayValue,
                    ExecuteAsync = () => VaultTimeoutActionAsync()
                },
                new SettingsPageListItem
                {
                    Name = AppResources.UnlockWithPIN,
                    SubLabel = _pin ? AppResources.On : AppResources.Off,
                    ExecuteAsync = () => UpdatePinAsync()
                },
                new SettingsPageListItem
                {
                    Name = AppResources.ApproveLoginRequests,
                    SubLabel = _approvePasswordlessLoginRequests ? AppResources.On : AppResources.Off,
                    ExecuteAsync = () => ApproveLoginRequestsAsync()
                },
                new SettingsPageListItem
                {
                    Name = AppResources.LockNow,
                    ExecuteAsync = () => LockAsync()
                },
                new SettingsPageListItem
                {
                    Name = AppResources.TwoStepLogin,
                    ExecuteAsync = () => TwoStepAsync()
                }
            };
            if (_approvePasswordlessLoginRequests)
            {
                manageItems.Add(new SettingsPageListItem
                {
                    Name = AppResources.PendingLogInRequests,
                    ExecuteAsync = () => PendingLoginRequestsAsync()
                });
            }
            if (_supportsBiometric || _biometric)
            {
                var biometricName = AppResources.Biometrics;
                // TODO Xamarin.Forms.Device.RuntimePlatform is no longer supported. Use Microsoft.Maui.Devices.DeviceInfo.Platform instead. For more details see https://learn.microsoft.com/en-us/dotnet/maui/migration/forms-projects#device-changes
                if (Device.RuntimePlatform == Device.iOS)
                {
                    biometricName = _deviceActionService.SupportsFaceBiometric() ? AppResources.FaceID :
                        AppResources.TouchID;
                }
                var item = new SettingsPageListItem
                {
                    Name = string.Format(AppResources.UnlockWith, biometricName),
                    SubLabel = _biometric ? AppResources.On : AppResources.Off,
                    ExecuteAsync = () => UpdateBiometricAsync()
                };
                securityItems.Insert(2, item);
            }
            if (_vaultTimeoutDisplayValue == AppResources.Custom)
            {
                securityItems.Insert(1, new SettingsPageListItem
                {
                    Name = AppResources.Custom,
                    Time = TimeSpan.FromMinutes(Math.Abs((double)_vaultTimeout.GetValueOrDefault())),
                });
            }
            if (_vaultTimeoutPolicy != null)
            {
                var policyMinutes = _vaultTimeoutPolicy.GetInt(Policy.MINUTES_KEY);
                var policyAction = _vaultTimeoutPolicy.GetString(Policy.ACTION_KEY);

                if (policyMinutes.HasValue || !string.IsNullOrWhiteSpace(policyAction))
                {
                    string policyAlert;
                    if (policyMinutes.HasValue && string.IsNullOrWhiteSpace(policyAction))
                    {
                        policyAlert = string.Format(AppResources.VaultTimeoutPolicyInEffect,
                            Math.Floor((float)policyMinutes / 60),
                            policyMinutes % 60);
                    }
                    else if (!policyMinutes.HasValue && !string.IsNullOrWhiteSpace(policyAction))
                    {
                        policyAlert = string.Format(AppResources.VaultTimeoutActionPolicyInEffect,
                            policyAction == Policy.ACTION_LOCK ? AppResources.Lock : AppResources.LogOut);
                    }
                    else
                    {
                        policyAlert = string.Format(AppResources.VaultTimeoutPolicyWithActionInEffect,
                            Math.Floor((float)policyMinutes / 60),
                            policyMinutes % 60,
                            policyAction == Policy.ACTION_LOCK ? AppResources.Lock : AppResources.LogOut);
                    }
                    securityItems.Insert(0, new SettingsPageListItem
                    {
                        Name = policyAlert,
                        UseFrame = true,
                    });
                }
            }
            // TODO Xamarin.Forms.Device.RuntimePlatform is no longer supported. Use Microsoft.Maui.Devices.DeviceInfo.Platform instead. For more details see https://learn.microsoft.com/en-us/dotnet/maui/migration/forms-projects#device-changes
            if (Device.RuntimePlatform == Device.Android)
            {
                securityItems.Add(new SettingsPageListItem
                {
                    Name = AppResources.AllowScreenCapture,
                    SubLabel = _screenCaptureAllowed ? AppResources.On : AppResources.Off,
                    ExecuteAsync = () => SetScreenCaptureAllowedAsync()
                });
            }
            var accountItems = new List<SettingsPageListItem>();
            // TODO Xamarin.Forms.Device.RuntimePlatform is no longer supported. Use Microsoft.Maui.Devices.DeviceInfo.Platform instead. For more details see https://learn.microsoft.com/en-us/dotnet/maui/migration/forms-projects#device-changes
            if (Device.RuntimePlatform == Device.iOS)
            {
                accountItems.Add(new SettingsPageListItem
                {
                    Name = AppResources.ConnectToWatch,
                    SubLabel = _shouldConnectToWatch ? AppResources.On : AppResources.Off,
                    ExecuteAsync = () => ToggleWatchConnectionAsync()
                });
            }
            accountItems.Add(new SettingsPageListItem
            {
                Name = AppResources.FingerprintPhrase,
                ExecuteAsync = () => FingerprintAsync()
            });
            accountItems.Add(new SettingsPageListItem
            {
                Name = AppResources.LogOut,
                ExecuteAsync = () => LogOutAsync()
            });
            if (_showChangeMasterPassword)
            {
                accountItems.Insert(0, new SettingsPageListItem
                {
                    Name = AppResources.ChangeMasterPassword,
                    ExecuteAsync = () => ChangePasswordAsync()
                });
            }
            var toolsItems = new List<SettingsPageListItem>
            {
                new SettingsPageListItem
                {
                    Name = AppResources.ImportItems,
                    ExecuteAsync = () => Device.InvokeOnMainThreadAsync(() => Import())
                },
                new SettingsPageListItem
                {
                    Name = AppResources.ExportVault,
                    ExecuteAsync = () => Page.Navigation.PushModalAsync(new NavigationPage(new ExportVaultPage()))
                }
            };
            if (IncludeLinksWithSubscriptionInfo())
            {
                toolsItems.Add(new SettingsPageListItem
                {
                    Name = AppResources.LearnOrg,
                    ExecuteAsync = () => ShareAsync()
                });
                toolsItems.Add(new SettingsPageListItem
                {
                    Name = AppResources.WebVault,
                    ExecuteAsync = () => Device.InvokeOnMainThreadAsync(() => WebVault())
                });
            }

            var otherItems = new List<SettingsPageListItem>
            {
                new SettingsPageListItem
                {
                    Name = AppResources.Options,
                    ExecuteAsync = () => Page.Navigation.PushModalAsync(new NavigationPage(new OptionsPage()))
                },
                new SettingsPageListItem
                {
                    Name = AppResources.About,
                    ExecuteAsync = () => AboutAsync()
                },
                new SettingsPageListItem
                {
                    Name = AppResources.HelpAndFeedback,
                    ExecuteAsync = () => Device.InvokeOnMainThreadAsync(() => Help())
                },
#if !FDROID 
                new SettingsPageListItem
                {
                    Name = AppResources.SubmitCrashLogs,
                    SubLabel = _reportLoggingEnabled ? AppResources.On : AppResources.Off,
                    ExecuteAsync = () => LoggerReportingAsync()
                },
#endif
                new SettingsPageListItem
                {
                    Name = AppResources.RateTheApp,
                    ExecuteAsync = () => Device.InvokeOnMainThreadAsync(() => Rate())
                },
                new SettingsPageListItem
                {
                    Name = AppResources.DeleteAccount,
                    ExecuteAsync = () => Page.Navigation.PushModalAsync(new NavigationPage(new DeleteAccountPage()))
                }
            };

            // TODO: improve this. Leaving this as is to reduce error possibility on the hotfix.
            var settingsListGroupItems = new List<SettingsPageListGroup>()
            {
                new SettingsPageListGroup(autofillItems, AppResources.Autofill, doUpper, true),
                new SettingsPageListGroup(manageItems, AppResources.Manage, doUpper),
                new SettingsPageListGroup(securityItems, AppResources.Security, doUpper),
                new SettingsPageListGroup(accountItems, AppResources.Account, doUpper),
                new SettingsPageListGroup(toolsItems, AppResources.Tools, doUpper),
                new SettingsPageListGroup(otherItems, AppResources.Other, doUpper)
            };

            // TODO: refactor this
            // TODO Xamarin.Forms.Device.RuntimePlatform is no longer supported. Use Microsoft.Maui.Devices.DeviceInfo.Platform instead. For more details see https://learn.microsoft.com/en-us/dotnet/maui/migration/forms-projects#device-changes
                        if (Device.RuntimePlatform == Device.Android
                ||
                GroupedItems.Any())
            {
                var items = new List<ISettingsPageListItem>();
                foreach (var itemGroup in settingsListGroupItems)
                {
                    items.Add(new SettingsPageHeaderListItem(itemGroup.Name));
                    items.AddRange(itemGroup);
                }

                GroupedItems.ReplaceRange(items);
            }
            else
            {
                // HACK: we need this on iOS, so that it doesn't crash when adding coming from an empty list
                var first = true;
                var items = new List<ISettingsPageListItem>();
                foreach (var itemGroup in settingsListGroupItems)
                {
                    if (!first)
                    {
                        items.Add(new SettingsPageHeaderListItem(itemGroup.Name));
                    }
                    else
                    {
                        first = false;
                    }
                    items.AddRange(itemGroup);
                }

                if (settingsListGroupItems.Any())
                {
                    GroupedItems.ReplaceRange(new List<ISettingsPageListItem> { new SettingsPageHeaderListItem(settingsListGroupItems[0].Name) });
                    GroupedItems.AddRange(items);
                }
                else
                {
                    GroupedItems.Clear();
                }
            }
        }

        private async Task PendingLoginRequestsAsync()
        {
            try
            {
                var requests = await _authService.GetActivePasswordlessLoginRequestsAsync();
                if (requests == null || !requests.Any())
                {
                    _platformUtilsService.ShowToast("info", null, AppResources.NoPendingRequests);
                    return;
                }

                Page.Navigation.PushModalAsync(new NavigationPage(new LoginPasswordlessRequestsListPage())).FireAndForget();
            }
            catch (Exception ex)
            {
                HandleException(ex);
            }
        }

        private bool IncludeLinksWithSubscriptionInfo()
        {
            // TODO Xamarin.Forms.Device.RuntimePlatform is no longer supported. Use Microsoft.Maui.Devices.DeviceInfo.Platform instead. For more details see https://learn.microsoft.com/en-us/dotnet/maui/migration/forms-projects#device-changes
            if (Device.RuntimePlatform == Device.iOS)
            {
                return false;
            }
            return true;
        }

        private VaultTimeoutAction GetVaultTimeoutActionFromKey(string key)
        {
            return _vaultTimeoutActionOptions.FirstOrDefault(o => o.Key == key).Value;
        }

        private int? GetVaultTimeoutFromKey(string key)
        {
            return _vaultTimeoutOptions.FirstOrDefault(o => o.Key == key).Value;
        }

        private string CreateSelectableOption(string option, bool selected) => selected ? $"✓ {option}" : option;

        private bool CompareSelection(string selection, string compareTo) => selection == compareTo || selection == $"✓ {compareTo}";

        public async Task SetScreenCaptureAllowedAsync()
        {
            try
            {
                if (!_screenCaptureAllowed
                    &&
                    !await Page.DisplayAlert(AppResources.AllowScreenCapture, AppResources.AreYouSureYouWantToEnableScreenCapture, AppResources.Yes, AppResources.No))
                {
                    return;
                }

                await _stateService.SetScreenCaptureAllowedAsync(!_screenCaptureAllowed);
                _screenCaptureAllowed = !_screenCaptureAllowed;
                await _deviceActionService.SetScreenCaptureAllowedAsync();
                BuildList();
            }
            catch (Exception ex)
            {
                _loggerService.Exception(ex);
                await Page.DisplayAlert(AppResources.AnErrorHasOccurred, AppResources.GenericErrorMessage, AppResources.Ok);
            }
        }

        private async Task ToggleWatchConnectionAsync()
        {
            _shouldConnectToWatch = !_shouldConnectToWatch;

            await _watchDeviceService.SetShouldConnectToWatchAsync(_shouldConnectToWatch);
            BuildList();
        }
    }
}
