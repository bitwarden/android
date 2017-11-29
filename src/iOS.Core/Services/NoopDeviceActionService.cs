using System;
using Bit.App.Abstractions;
using System.Threading.Tasks;
using Bit.App.Models.Page;

namespace Bit.iOS.Core.Services
{
    public class NoopDeviceActionService : IDeviceActionService
    {
        public void Autofill(VaultListPageModel.Cipher cipher)
        {
            // do nothing
        }

        public void Background()
        {
            // do nothing
        }

        public bool CanOpenFile(string fileName)
        {
            return false;
        }

        public void ClearCache()
        {
            // do nothing
        }

        public void CloseAutofill()
        {
            // do nothing
        }

        public void CopyToClipboard(string text)
        {
            // do nothing
        }

        public void DismissKeyboard()
        {
            // do nothing
        }

        public void LaunchApp(string appName)
        {
            // do nothing
        }

        public void OpenAccessibilitySettings()
        {
            // do nothing
        }

        public void OpenAutofillSettings()
        {
            // do nothing
        }

        public bool OpenFile(byte[] fileData, string id, string fileName)
        {
            return false;
        }

        public void RateApp()
        {
            // do nothing
        }

        public Task SelectFileAsync()
        {
            return Task.FromResult(0);
        }
    }
}
