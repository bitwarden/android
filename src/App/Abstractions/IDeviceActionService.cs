using Bit.Core.Enums;
using Bit.Core.Models.View;
using System.Threading.Tasks;

namespace Bit.App.Abstractions
{
    public interface IDeviceActionService
    {
        string DeviceUserAgent { get; }
        DeviceType DeviceType { get; }
        void Toast(string text, bool longDuration = false);
        bool LaunchApp(string appName);
        Task ShowLoadingAsync(string text);
        Task HideLoadingAsync();
        bool OpenFile(byte[] fileData, string id, string fileName);
        bool SaveFile(byte[] fileData, string id, string fileName, string contentUri);
        bool CanOpenFile(string fileName);
        Task ClearCacheAsync();
        Task SelectFileAsync();
        Task<string> DisplayPromptAync(string title = null, string description = null, string text = null,
            string okButtonText = null, string cancelButtonText = null, bool numericKeyboard = false,
            bool autofocus = true, bool password = false);
        void RateApp();
        bool SupportsFaceBiometric();
        Task<bool> SupportsFaceBiometricAsync();
        bool SupportsNfc();
        bool SupportsCamera();
        bool SupportsAutofillService();
        int SystemMajorVersion();
        string SystemModel();
        Task<string> DisplayAlertAsync(string title, string message, string cancel, params string[] buttons);
        Task<string> DisplayActionSheetAsync(string title, string cancel, string destruction, params string[] buttons);
        void Autofill(CipherView cipher);
        void CloseAutofill();
        void Background();
        bool AutofillAccessibilityServiceRunning();
        bool AutofillAccessibilityOverlayPermitted();
        bool AutofillServiceEnabled();
        void DisableAutofillService();
        bool AutofillServicesEnabled();
        string GetBuildNumber();
        void OpenAccessibilitySettings();
        void OpenAccessibilityOverlayPermissionSettings();
        void OpenAutofillSettings();
        bool UsingDarkTheme();
        long GetActiveTime();
        void CloseMainApp();
    }
}
