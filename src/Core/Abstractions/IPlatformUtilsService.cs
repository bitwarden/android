using Bit.Core.Enums;

namespace Bit.Core.Abstractions
{
    public interface IPlatformUtilsService
    {
        string GetApplicationVersion();
        /// <summary>
        /// Gets the device type on the server enum
        /// </summary>
        Enums.DeviceType GetDevice();
        string GetDeviceString();
        ClientType GetClientType();
        bool IsSelfHost();
        bool IsViewOpen();
        void LaunchUri(string uri, Dictionary<string, object> options = null);
        Task<string> ReadFromClipboardAsync(Dictionary<string, object> options = null);
        Task<bool> ShowDialogAsync(string text, string title = null, string confirmText = null,
            string cancelText = null, string type = null);
        Task<bool> ShowPasswordDialogAsync(string title, string body, Func<string, Task<bool>> validator);
        Task<(string password, bool valid)> ShowPasswordDialogAndGetItAsync(string title, string body, Func<string, Task<bool>> validator);
        void ShowToast(string type, string title, string text, Dictionary<string, object> options = null);
        void ShowToast(string type, string title, string[] text, Dictionary<string, object> options = null);
        void ShowToastForCopiedValue(string valueNameCopied);
        bool SupportsFido2();
        bool SupportsDuo();
        Task<bool> SupportsBiometricAsync();
        Task<bool> IsBiometricIntegrityValidAsync(string bioIntegritySrcKey = null);
        Task<bool?> AuthenticateBiometricAsync(string text = null, string fallbackText = null, Action fallback = null, bool logOutOnTooManyAttempts = false, bool allowAlternativeAuthentication = false);
        long GetActiveTime();
    }
}
