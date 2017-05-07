using System;
using Xamarin.Android.Net;
using Bit.App;
using Bit.App.Abstractions;

namespace Bit.Android.Services
{
    public class HttpService : IHttpService
    {
        public ApiHttpClient ApiClient => new ApiHttpClient(new AndroidClientHandler());
        public IdentityHttpClient IdentityClient => new IdentityHttpClient(new AndroidClientHandler());
    }
}
