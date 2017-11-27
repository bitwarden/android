using Bit.App.Abstractions;
using Windows.ApplicationModel;

namespace Bit.UWP.Services
{
    public class AppInfoService : IAppInfoService
    {
        public string Build => Package.Current.Id.Version.Build.ToString();

        public string Version
        {
            get
            {
                var version = Package.Current.Id.Version;
                return $"{version.Major}.{version.Minor}.{version.Build}";
            }
        }

        public bool AutofillAccessibilityServiceEnabled => false;
        public bool AutofillServiceEnabled => false;
    }
}
