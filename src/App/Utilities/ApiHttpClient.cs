using System.Net.Http;
using System;
using System.Net.Http.Headers;

namespace Bit.App
{
    public class ApiHttpClient : HttpClient
    {
        public ApiHttpClient()
        {
            BaseAddress = new Uri("https://api.bitwarden.com");
            DefaultRequestHeaders.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));
        }
    }
}
