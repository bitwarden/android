using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Resources.Localization;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Plugin.Fingerprint;
using Plugin.Fingerprint.Abstractions;

namespace Bit.App.Services
{
    public class MobilePlatformUtilsService : IPlatformUtilsService
    {
        private static readonly Random _random = new Random();

        private const int DialogPromiseExpiration = 600000; // 10 minutes

        private readonly IDeviceActionService _deviceActionService;
        private readonly IClipboardService _clipboardService;
        private readonly IMessagingService _messagingService;
        private readonly IBroadcasterService _broadcasterService;

        private readonly Dictionary<int, Tuple<TaskCompletionSource<bool>, DateTime>> _showDialogResolves =
            new Dictionary<int, Tuple<TaskCompletionSource<bool>, DateTime>>();

        public MobilePlatformUtilsService(
            IDeviceActionService deviceActionService,
            IClipboardService clipboardService,
            IMessagingService messagingService,
            IBroadcasterService broadcasterService
            )
        {
            _deviceActionService = deviceActionService;
            _clipboardService = clipboardService;
            _messagingService = messagingService;
            _broadcasterService = broadcasterService;
        }

        public void Init()
        {
            _broadcasterService.Subscribe(nameof(MobilePlatformUtilsService), (message) =>
            {
                if (message.Command == "showDialogResolve")
                {
                    var details = message.Data as Tuple<int, bool>;
                    var dialogId = details.Item1;
                    var confirmed = details.Item2;
                    if (_showDialogResolves.ContainsKey(dialogId))
                    {
                        var resolveObj = _showDialogResolves[dialogId].Item1;
                        resolveObj.TrySetResult(confirmed);
                    }

                    // Clean up old tasks
                    var deleteIds = new HashSet<int>();
                    foreach (var item in _showDialogResolves)
                    {
                        var age = DateTime.UtcNow - item.Value.Item2;
                        if (age.TotalMilliseconds > DialogPromiseExpiration)
                        {
                            deleteIds.Add(item.Key);
                        }
                    }
                    foreach (var id in deleteIds)
                    {
                        _showDialogResolves.Remove(id);
                    }
                }
            });
        }

        /// <summary>
        /// Gets the device type on the server enum
        /// </summary>
        public Core.Enums.DeviceType GetDevice()
        {
            // Can't use Device.RuntimePlatform here because it gets called before Forms.Init() and throws.
            // so we need to get the DeviceType ourselves
            return _deviceActionService.DeviceType;
        }

        public string GetDeviceString()
        {
            return DeviceInfo.Model;
        }

        public ClientType GetClientType()
        {
            return ClientType.Mobile;
        }

        public bool IsViewOpen()
        {
            return false;
        }

        public void LaunchUri(string uri, Dictionary<string, object> options = null)
        {
            if ((uri.StartsWith("http://") || uri.StartsWith("https://")) &&
                Uri.TryCreate(uri, UriKind.Absolute, out var parsedUri))
            {
                try
                {
                    Browser.OpenAsync(uri, BrowserLaunchMode.External);
                }
                catch (FeatureNotSupportedException) { }
            }
            else
            {
                var launched = false;
                if (GetDevice() == Core.Enums.DeviceType.Android && uri.StartsWith("androidapp://"))
                {
                    launched = _deviceActionService.LaunchApp(uri);
                }
                if (!launched && (options?.ContainsKey("page") ?? false))
                {
                    (options["page"] as Page).DisplayAlert(null, "", ""); // TODO
                }
            }
        }

        public string GetApplicationVersion()
        {
            return AppInfo.VersionString;
        }

        public bool SupportsDuo()
        {
            return true;
        }

        public void ShowToastForCopiedValue(string valueNameCopied)
        {
            ShowToast("info", null, string.Format(AppResources.ValueHasBeenCopied, valueNameCopied));
        }

        public bool SupportsFido2()
        {
            return _deviceActionService.SupportsFido2();
        }

        public void ShowToast(string type, string title, string text, Dictionary<string, object> options = null)
        {
            ShowToast(type, title, new string[] { text }, options);
        }

        public void ShowToast(string type, string title, string[] text, Dictionary<string, object> options = null)
        {
            if (text.Length > 0)
            {
                var longDuration = options != null && options.ContainsKey("longDuration") ?
                    (bool)options["longDuration"] : false;
                _deviceActionService.Toast(text[0], longDuration);
            }
        }

        public Task<bool> ShowDialogAsync(string text, string title = null, string confirmText = null,
            string cancelText = null, string type = null)
        {
            var dialogId = 0;
            lock (_random)
            {
                dialogId = _random.Next(0, int.MaxValue);
            }
            _messagingService.Send("showDialog", new DialogDetails
            {
                Text = text,
                Title = title,
                ConfirmText = confirmText,
                CancelText = cancelText,
                Type = type,
                DialogId = dialogId
            });
            var tcs = new TaskCompletionSource<bool>();
            _showDialogResolves.Add(dialogId, new Tuple<TaskCompletionSource<bool>, DateTime>(tcs, DateTime.UtcNow));
            return tcs.Task;
        }

        public async Task<bool> ShowPasswordDialogAsync(string title, string body, Func<string, Task<bool>> validator)
        {
            return (await ShowPasswordDialogAndGetItAsync(title, body, validator)).valid;
        }

        public async Task<(string password, bool valid)> ShowPasswordDialogAndGetItAsync(string title, string body, Func<string, Task<bool>> validator)
        {
            var password = await _deviceActionService.DisplayPromptAync(AppResources.PasswordConfirmation,
                AppResources.PasswordConfirmationDesc, null, AppResources.Submit, AppResources.Cancel, password: true);

            if (password == null)
            {
                return (password, false);
            }

            var valid = await validator(password);

            if (!valid)
            {
                await ShowDialogAsync(AppResources.InvalidMasterPassword, null, AppResources.Ok);
            }

            return (password, valid);
        }

        public bool IsSelfHost()
        {
            return false;
        }

        public async Task<string> ReadFromClipboardAsync(Dictionary<string, object> options = null)
        {
            return await Clipboard.GetTextAsync();
        }

        public async Task<bool> SupportsBiometricAsync()
        {
            try
            {
                return await CrossFingerprint.Current.IsAvailableAsync();
            }
            catch
            {
                return false;
            }
        }

        public async Task<bool> IsBiometricIntegrityValidAsync(string bioIntegritySrcKey = null)
        {
            bioIntegritySrcKey ??= Core.Constants.BiometricIntegritySourceKey;

            var biometricService = ServiceContainer.Resolve<IBiometricService>();
            if (!await biometricService.IsSystemBiometricIntegrityValidAsync(bioIntegritySrcKey))
            {
                return false;
            }

            var stateService = ServiceContainer.Resolve<IStateService>();
            return await stateService.IsAccountBiometricIntegrityValidAsync(bioIntegritySrcKey);
        }

        public async Task<bool?> AuthenticateBiometricAsync(string text = null, string fallbackText = null,
            Action fallback = null, bool logOutOnTooManyAttempts = false, bool allowAlternativeAuthentication = false)
        {
            try
            {
                if (text == null)
                {
                    text = AppResources.BiometricsDirection;
#if IOS
                    var supportsFace = await _deviceActionService.SupportsFaceBiometricAsync();
                    text = supportsFace ? AppResources.FaceIDDirection : AppResources.FingerprintDirection;
#endif
                }
                var biometricRequest = new AuthenticationRequestConfiguration(AppResources.Bitwarden, text)
                {
                    CancelTitle = AppResources.Cancel,
                    FallbackTitle = fallbackText,
                    AllowAlternativeAuthentication = allowAlternativeAuthentication
                };
                var result = await CrossFingerprint.Current.AuthenticateAsync(biometricRequest);
                if (result.Authenticated)
                {
                    return true;
                }
                if (result.Status == FingerprintAuthenticationResultStatus.Canceled)
                {
                    return null;
                }
                if (result.Status == FingerprintAuthenticationResultStatus.FallbackRequested)
                {
                    fallback?.Invoke();
                }
                if (result.Status == FingerprintAuthenticationResultStatus.TooManyAttempts
                    && logOutOnTooManyAttempts)
                {
                    await ShowDialogAsync(AppResources.AccountLoggedOutBiometricExceeded, AppResources.TooManyAttempts, AppResources.Ok);
                    _messagingService.Send(AccountsManagerMessageCommands.LOGOUT);
                }
            }
            catch { }
            return false;
        }

        public long GetActiveTime()
        {
            return _deviceActionService.GetActiveTime();
        }
    }
}
