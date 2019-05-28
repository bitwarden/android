using Bit.Core.Enums;
using Bit.Core.Models.View;
using System.Threading.Tasks;

namespace Bit.App.Abstractions
{
    public interface IDeviceActionService
    {
        DeviceType DeviceType { get; }
        void Toast(string text, bool longDuration = false);
        bool LaunchApp(string appName);
        Task ShowLoadingAsync(string text);
        Task HideLoadingAsync();
        bool OpenFile(byte[] fileData, string id, string fileName);
        bool CanOpenFile(string fileName);
        Task ClearCacheAsync();
        Task SelectFileAsync();
        Task<string> DisplayPromptAync(string title = null, string description = null, string text = null,
            string okButtonText = null, string cancelButtonText = null, bool numericKeyboard = false);
        void RateApp();
        bool SupportsFaceId();
        bool SupportsNfc();
        bool SupportsCamera();
        bool SupportsAutofillService();
        int SystemMajorVersion();
        string SystemModel();
        Task<string> DisplayAlertAsync(string title, string message, string cancel, params string[] buttons);
        void Autofill(CipherView cipher);
        void CloseAutofill();
        void Background();
    }
}
