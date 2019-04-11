using System;
using System.IO;
using Android.App;
using Android.Runtime;
using Bit.App.Abstractions;
using Bit.App.Services;
using Bit.Core.Abstractions;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Bit.Droid.Services;

namespace Bit.Droid
{
#if DEBUG
    [Application(Debuggable = true)]
#else
    [Application(Debuggable = false)]
#endif
    [Register("com.x8bit.bitwarden.MainApplication")]
    public class MainApplication : Application
    {
        public MainApplication(IntPtr handle, JniHandleOwnership transer)
          : base(handle, transer)
        {
            if(ServiceContainer.RegisteredServices.Count == 0)
            {
                RegisterLocalServices();
                ServiceContainer.Init();
            }
        }

        public override void OnCreate()
        {
            base.OnCreate();
            Plugin.CurrentActivity.CrossCurrentActivity.Current.Init(this);
        }

        public void RegisterLocalServices()
        {
            var preferencesStorage = new PreferencesStorageService(null);
            var documentsPath = Environment.GetFolderPath(Environment.SpecialFolder.Personal);
            var liteDbStorage = new LiteDbStorageService(Path.Combine(documentsPath, "bitwarden.db"));
            var deviceActionService = new DeviceActionService();

            ServiceContainer.Register<ICryptoPrimitiveService>("cryptoPrimitiveService", new CryptoPrimitiveService());
            ServiceContainer.Register<IStorageService>("storageService",
                new MobileStorageService(preferencesStorage, liteDbStorage));
            ServiceContainer.Register<IStorageService>("secureStorageService", new SecureStorageService());
            ServiceContainer.Register<IDeviceActionService>("deviceActionService", deviceActionService);
            ServiceContainer.Register<IPlatformUtilsService>("platformUtilsService",
                new MobilePlatformUtilsService(deviceActionService));
        }
    }
}