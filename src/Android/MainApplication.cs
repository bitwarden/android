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
            var localizeService = new LocalizeService();
            var messagingService = new MobileMessagingService();
            var i18nService = new MobileI18nService(localizeService.GetCurrentCultureInfo());
            var secureStorageService = new SecureStorageService();
            var cryptoPrimitiveService = new CryptoPrimitiveService();
            var mobileStorageService = new MobileStorageService(preferencesStorage, liteDbStorage);
            var platformUtilsService = new MobilePlatformUtilsService(deviceActionService);

            ServiceContainer.Register<IMessagingService>("messagingService", messagingService);
            ServiceContainer.Register<ILocalizeService>("localizeService", localizeService);
            ServiceContainer.Register<II18nService>("i18nService", i18nService);
            ServiceContainer.Register<ICryptoPrimitiveService>("cryptoPrimitiveService", cryptoPrimitiveService);
            ServiceContainer.Register<IStorageService>("storageService", mobileStorageService);
            ServiceContainer.Register<IStorageService>("secureStorageService", secureStorageService);
            ServiceContainer.Register<IDeviceActionService>("deviceActionService", deviceActionService);
            ServiceContainer.Register<IPlatformUtilsService>("platformUtilsService", platformUtilsService);
        }
    }
}