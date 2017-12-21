using Bit.App.Abstractions;
using Microsoft.Toolkit.Uwp.Helpers;
using System;
using System.Linq;
using System.Threading.Tasks;
using Windows.Graphics.Display;
using Windows.Devices.SmartCards;
using Windows.Devices.Enumeration;

namespace Bit.UWP.Services
{
    public class DeviceInfoService : IDeviceInfoService
    {
        private const string SmartCardEmulatorType = "Windows.Devices.SmartCards.SmartCardEmulator";

        public string Type => Xamarin.Forms.Device.UWP;
        public string Model => SystemInformation.DeviceModel;
        public int Version => SystemInformation.OperatingSystemVersion.Build;
        public float Scale => (float)DisplayInformation.GetForCurrentView().RawPixelsPerViewPixel;

        public bool NfcEnabled
        {
            get
            {
                if(!Windows.Foundation.Metadata.ApiInformation.IsTypePresent(SmartCardEmulatorType))
                {
                    return false;
                }

                return Task.Run(async () => await SmartCardEmulator.GetDefaultAsync()).Result != null;
            }
        }

        public bool HasCamera
        {
            get
            {
                var cameraList = Task.Run(async () =>
                    await DeviceInformation.FindAllAsync(DeviceClass.VideoCapture)).Result;
                return cameraList?.Any() ?? false;
            }
        }
        
        public bool AutofillServiceSupported => false;
        public bool HasFaceIdSupport => false;
    }
}
