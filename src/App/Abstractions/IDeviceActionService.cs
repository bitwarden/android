using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.Core.Enums;
using Bit.Core.Models.View;
using static Bit.App.Pages.SettingsPageViewModel;

namespace Bit.App.Abstractions
{
    public interface IDeviceActionService
    {
        string DeviceUserAgent { get; }
        DeviceType DeviceType { get; }
        bool IsWatchReachable { get; }
        int SystemMajorVersion();
        string SystemModel();
        string GetBuildNumber();

        void Toast(string text, bool longDuration = false);
        Task ShowLoadingAsync(string text);
        Task HideLoadingAsync();
        Task<string> DisplayPromptAync(string title = null, string description = null, string text = null,
            string okButtonText = null, string cancelButtonText = null, bool numericKeyboard = false,
            bool autofocus = true, bool password = false);
        Task<string> DisplayAlertAsync(string title, string message, string cancel, params string[] buttons);
        Task<string> DisplayActionSheetAsync(string title, string cancel, string destruction, params string[] buttons);

        bool SupportsFaceBiometric();
        Task<bool> SupportsFaceBiometricAsync();
        bool SupportsNfc();
        bool SupportsCamera();
        bool SupportsFido2();

        bool LaunchApp(string appName);
        void RateApp();
        void OpenAccessibilitySettings();
        void OpenAccessibilityOverlayPermissionSettings();
        void OpenAutofillSettings();
        long GetActiveTime();
        void CloseMainApp();
        float GetSystemFontSizeScale();
        Task OnAccountSwitchCompleteAsync();
        Task SetScreenCaptureAllowedAsync();
        void OpenAppSettings();
        Task SendDataToWatchAsync(WatchDTO dto);
        void CloseExtensionPopUp();
    }
}
