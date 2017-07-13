using System.Net.Http;
using System;
using System.Net.Http.Headers;

namespace Bit.App
{
    public class ApiHttpClient : HttpClient
    {
        public ApiHttpClient()
        {
            Init();
        }

        public ApiHttpClient(HttpMessageHandler handler)
            : base(handler)
        {
            Init();
        }

        private void Init()
        {
            //BaseAddress = new Uri("http://169.254.80.80:4000"); // Desktop from VS Android Emulator
            //BaseAddress = new Uri("http://192.168.1.3:4000"); // Desktop
            //BaseAddress = new Uri("https://preview-api.bitwarden.com"); // Preview
            BaseAddress = new Uri("https://api.bitwarden.com"); // Production
            DefaultRequestHeaders.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));
        }
    }
}
