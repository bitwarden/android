using Bit.App.Abstractions;
using Bit.App.Models.Page;
using Coding4Fun.Toolkit.Controls;
using System;
using System.Linq;
using System.Runtime.InteropServices.WindowsRuntime;
using System.Threading.Tasks;
using Windows.ApplicationModel.Core;
using Windows.ApplicationModel.DataTransfer;
using Windows.Storage;
using Windows.System;
using Windows.UI;
using Windows.UI.Core;
using Windows.UI.Xaml;
using Windows.UI.Xaml.Media;

namespace Bit.UWP.Services
{
    public class DeviceActionService : IDeviceActionService
    {
        public bool CanOpenFile(string fileName) => true;

        public void ClearCache()
        {
            Task.Run(async () =>
            {
                foreach(var item in await ApplicationData.Current.LocalCacheFolder.GetItemsAsync())
                {
                    await item.DeleteAsync();
                }
            }).Wait();
        }

        public void CopyToClipboard(string text)
        {
            var dataPackage = new DataPackage
            {
                RequestedOperation = DataPackageOperation.Copy
            };
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
                if(file != null)
                {
                    await SelectFileResult(file);
                }
            }).AsTask();
        }

        private async Task SelectFileResult(StorageFile file)
        {
            var buffer = await FileIO.ReadBufferAsync(file);
            Xamarin.Forms.MessagingCenter.Send(Xamarin.Forms.Application.Current, "SelectFileResult",
                new Tuple<byte[], string>(buffer.ToArray(), file.Name));
        }

        public void Autofill(VaultListPageModel.Cipher cipher)
        {
            throw new NotImplementedException();
        }

        public void CloseAutofill()
        {
            throw new NotImplementedException();
        }

        public void Background()
        {
            // do nothing
        }

        public void RateApp()
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
            throw new NotImplementedException();
        }

        public void OpenAutofillSettings()
        {
            throw new NotImplementedException();
        }

        public void Toast(string text, bool longDuration = false)
        {
            new ToastPrompt
            {
                Message = text,
                TextWrapping = TextWrapping.Wrap,
                MillisecondsUntilHidden = Convert.ToInt32(longDuration ? 5 : 2) * 1000,
                Background = new SolidColorBrush(Color.FromArgb(255, 73, 73, 73)),
                Foreground = new SolidColorBrush(Colors.White),
                Margin = new Thickness(0, 0, 0, 100),
                HorizontalAlignment = HorizontalAlignment.Center,
                VerticalAlignment = VerticalAlignment.Bottom,
                HorizontalContentAlignment = HorizontalAlignment.Center,
                VerticalContentAlignment = VerticalAlignment.Center,
                Stretch = Stretch.Uniform,
                IsHitTestVisible = false
            }.Show();
        }
    }
}
