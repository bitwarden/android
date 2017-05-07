using System.Net.Http;
using System;
using System.Net.Http.Headers;

namespace Bit.App
{
    public class IdentityHttpClient : HttpClient
    {
        public IdentityHttpClient()
        {
            Init();
        }

        public IdentityHttpClient(HttpMessageHandler handler)
            : base(handler)
        {
            Init();
        }

        private void Init()
        {
            //BaseAddress = new Uri("http://169.254.80.80:33656"); // Desktop from VS Android Emulator
            //BaseAddress = new Uri("http://192.168.1.8:33656"); // Desktop
            //BaseAddress = new Uri("https://identity-api.bitwarden.com"); // Preview
            BaseAddress = new Uri("https://api.bitwarden.com"); // Production
            DefaultRequestHeaders.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));
        }
    }
}
