using System;
using Bit.App.Abstractions;
using UIKit;

namespace Bit.iOS.Services
{
    public class DeviceActionService : IDeviceActionService
    {
        public void CopyToClipboard(string text)
        {
            UIPasteboard clipboard = UIPasteboard.General;
            clipboard.String = text;
        }

        public bool OpenFile(byte[] fileData, string id, string fileName)
        {
            return true;
        }

        public bool CanOpenFile(string fileName)
        {
            return true;
        }
    }
}
