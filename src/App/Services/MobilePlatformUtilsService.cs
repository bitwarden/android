using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.App.Resources;
using Bit.Core.Abstractions;
using Plugin.Fingerprint;
using Plugin.Fingerprint.Abstractions;
using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Xamarin.Essentials;
using Xamarin.Forms;

namespace Bit.App.Services
{
    public class MobilePlatformUtilsService : IPlatformUtilsService
    {
        private static readonly Random _random = new Random();

        private const int DialogPromiseExpiration = 600000; // 10 minutes

        private readonly IDeviceActionService _deviceActionService;
        private readonly IMessagingService _messagingService;
        private readonly IBroadcasterService _broadcasterService;
        private readonly Dictionary<int, Tuple<TaskCompletionSource<bool>, DateTime>> _showDialogResolves =
            new Dictionary<int, Tuple<TaskCompletionSource<bool>, DateTime>>();

        public MobilePlatformUtilsService(
            IDeviceActionService deviceActionService,
            IMessagingService messagingService,
            IBroadcasterService broadcasterService)
        {
            _deviceActionService = deviceActionService;
            _messagingService = messagingService;
            _broadcasterService = broadcasterService;
        }

        public string IdentityClientId => "mobile";

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

        public Core.Enums.DeviceType GetDevice()
        {
            return _deviceActionService.DeviceType;
        }

        public string GetDeviceString()
        {
            return DeviceInfo.Model;
        }

        public bool IsViewOpen()
        {
            return false;
        }

        public int? LockTimeout()
        {
            return null;
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

        public void SaveFile()
        {
            // TODO
        }

        public string GetApplicationVersion()
        {
            return AppInfo.VersionString;
        }

        public bool SupportsDuo()
        {
            return true;
        }

        public bool SupportsU2f()
        {
            return false;
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
            var password = await _deviceActionService.DisplayPromptAync(AppResources.PasswordConfirmation,
                AppResources.PasswordConfirmationDesc, null, AppResources.Submit, AppResources.Cancel, password: true);

            if (password == null)
            {
                return false;
            }

            var valid = await validator(password);

            if (!valid)
            {
                await ShowDialogAsync(AppResources.InvalidMasterPassword, null, AppResources.Ok);
            }

            return valid;
        }

        public bool IsDev()
        {
            return Core.Utilities.CoreHelpers.InDebugMode();
        }

        public bool IsSelfHost()
        {
            return false;
        }

        public async Task CopyToClipboardAsync(string text, Dictionary<string, object> options = null)
        {
            var clearMs = options != null && options.ContainsKey("clearMs") ? (int?)options["clearMs"] : null;
            var clearing = options != null && options.ContainsKey("clearing") ? (bool)options["clearing"] : false;
            await Clipboard.SetTextAsync(text);
            if (!clearing)
            {
                _messagingService.Send("copiedToClipboard", new Tuple<string, int?, bool>(text, clearMs, clearing));
            }
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

        public async Task<bool> AuthenticateBiometricAsync(string text = null, string fallbackText = null,
            Action fallback = null)
        {
            try
            {
                if (text == null)
                {
                    text = AppResources.BiometricsDirection;
                    if (Device.RuntimePlatform == Device.iOS)
                    {
                        var supportsFace = await _deviceActionService.SupportsFaceBiometricAsync();
                        text = supportsFace ? AppResources.FaceIDDirection : AppResources.FingerprintDirection;
                    }
                }
                var biometricRequest = new AuthenticationRequestConfiguration(AppResources.Bitwarden, text)
                {
                    CancelTitle = AppResources.Cancel,
                    FallbackTitle = fallbackText
                };
                var result = await CrossFingerprint.Current.AuthenticateAsync(biometricRequest);
                if (result.Authenticated)
                {
                    return true;
                }
                if (result.Status == FingerprintAuthenticationResultStatus.FallbackRequested)
                {
                    fallback?.Invoke();
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
