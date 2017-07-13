namespace Bit.App.Abstractions
{
    public interface IDeviceActionService
    {
        void CopyToClipboard(string text);
        bool OpenFile(byte[] fileData, string id, string fileName);
    }
}
