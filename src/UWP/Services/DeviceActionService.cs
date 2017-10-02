using Bit.App.Abstractions;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.InteropServices.WindowsRuntime;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Windows.ApplicationModel.Core;
using Windows.ApplicationModel.DataTransfer;
using Windows.Storage;
using Windows.System;
using Windows.UI.Core;
using Xamarin.Forms;

namespace Bit.UWP.Services
{
    public class DeviceActionService : IDeviceActionService
    {
        public bool CanOpenFile(string fileName)
        {
            return true;
        }

        public void ClearCache()
        {
            Task.Run(async () =>
            {
                foreach (var item in await ApplicationData.Current.LocalCacheFolder.GetItemsAsync())
                {
                    await item.DeleteAsync();
                }
            }).Wait();
        }

        public void CopyToClipboard(string text)
        {
            DataPackage dataPackage = new DataPackage();
            dataPackage.RequestedOperation = DataPackageOperation.Copy;
            dataPackage.SetText(text);

            Clipboard.SetContent(dataPackage);
        }

        public bool OpenFile(byte[] fileData, string id, string fileName)
        {
            try
            {
                //the method is synchronous in the interface, so the async method are run synchronously here
                var storageFolder = ApplicationData.Current.LocalCacheFolder;
                var file = storageFolder.CreateFileAsync(fileName, CreationCollisionOption.ReplaceExisting).AsTask().Result;
                FileIO.WriteBytesAsync(file, fileData).AsTask().Wait();
                Launcher.LaunchFileAsync(file, new LauncherOptions { DisplayApplicationPicker = true }).AsTask().Wait();

                return true;
            }
            catch
            {
                return false;
            }
        }

        public Task SelectFileAsync()
        {
            var picker = new Windows.Storage.Pickers.FileOpenPicker
            {
                ViewMode = Windows.Storage.Pickers.PickerViewMode.Thumbnail,
                SuggestedStartLocation = Windows.Storage.Pickers.PickerLocationId.PicturesLibrary,
                FileTypeFilter = { "*" }
            };

            return CoreApplication.MainView.CoreWindow.Dispatcher.RunAsync(CoreDispatcherPriority.Normal, async () =>
            {
                var file = await picker.PickSingleFileAsync();
                if (file != null)
                    await SelectFileResult(file);
            }).AsTask();
        }

        private async Task SelectFileResult(StorageFile file)
        {
            var buffer = await FileIO.ReadBufferAsync(file);

            MessagingCenter.Send(Xamarin.Forms.Application.Current, "SelectFileResult",
                new Tuple<byte[], string>(buffer.ToArray(), file.Name));
        }
    }
}
