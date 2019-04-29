using Bit.Core.Enums;
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
    }
}