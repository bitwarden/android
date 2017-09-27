using Bit.App.Abstractions;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Bit.UWP.Services
{
    public class AppInfoService : IAppInfoService
    {
        public string Build => throw new NotImplementedException();

        public string Version => throw new NotImplementedException();

        public bool AutofillServiceEnabled => throw new NotImplementedException();
    }
}
