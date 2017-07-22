using System;

namespace Bit.App.Abstractions
{
    public interface IDeviceActionService
    {
        void CopyToClipboard(string text);
        bool OpenFile(byte[] fileData, string id, string fileName);
        bool CanOpenFile(string fileName);
        void SelectFile();
        void ClearCache();
    }
}
