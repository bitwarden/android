using System;
using System.Threading.Tasks;

namespace Bit.App.Abstractions
{
    public interface IDeviceActionService
    {
        void Toast(string text, bool longDuration = false);
        void CopyToClipboard(string text);
        bool OpenFile(byte[] fileData, string id, string fileName);
        bool CanOpenFile(string fileName);
        Task SelectFileAsync();
        void ClearCache();
        void Autofill(Models.Page.VaultListPageModel.Cipher cipher);
        void CloseAutofill();
        void Background();
        void RateApp();
        void DismissKeyboard();
        void OpenAccessibilitySettings();
        void OpenAutofillSettings();
        void LaunchApp(string appName);
    }
}
