using System;
using System.Threading.Tasks;
using Xamarin.Forms;

namespace Bit.App.Abstractions
{
    public interface IDeviceActionService
    {
        Task ShowLoadingAsync(string text);
        Task HideLoadingAsync();
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
        Task LaunchAppAsync(string appName, Page page);
        Task<string> DisplayPromptAync(string title = null, string description = null, string text = null);
        Task<string> DisplayAlertAsync(string title, string message, string cancel, params string[] buttons);
    }
}
