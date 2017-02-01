using System;
using Bit.App.Abstractions;
using Foundation;

namespace Bit.iOS.Services
{
    public class AppInfoService : IAppInfoService
    {
        public string Build => NSBundle.MainBundle.InfoDictionary["CFBundleVersion"].ToString();
        public string Version => NSBundle.MainBundle.InfoDictionary["CFBundleShortVersionString"].ToString();
        public bool AutofillServiceEnabled => false;
    }
}
