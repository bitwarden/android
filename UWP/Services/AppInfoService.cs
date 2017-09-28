using Bit.App.Abstractions;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Windows.ApplicationModel;

namespace Bit.UWP.Services
{
    public class AppInfoService : IAppInfoService
    {

        public string Build
        {
            get
            {
                Package package = Package.Current;
                PackageId packageId = package.Id;
                PackageVersion version = packageId.Version;

                return version.Build.ToString();
            }
        }

        public string Version
        {
            get
            {
                Package package = Package.Current;
                PackageId packageId = package.Id;
                PackageVersion version = packageId.Version;

                return $"{version.Major}.{version.Minor}.{version.Build}";
            }
        }

        public bool AutofillServiceEnabled => false;
    }
}
