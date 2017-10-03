using Bit.App.Abstractions;
using Bit.App;

namespace Bit.UWP.Services
{
    public class HttpService : IHttpService
    {
        public ApiHttpClient ApiClient => new ApiHttpClient();
        public IdentityHttpClient IdentityClient => new IdentityHttpClient();
    }
}
