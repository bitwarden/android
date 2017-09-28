using Bit.App.Abstractions;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Bit.App;

namespace Bit.UWP.Services
{
    public class HttpService : IHttpService
    {
        public ApiHttpClient ApiClient => new ApiHttpClient();
        public IdentityHttpClient IdentityClient => new IdentityHttpClient();
    }
}
