using System.Net.Http;
using System;
using System.Net.Http.Headers;
using XLabs.Ioc;
using Bit.App.Abstractions;

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
            DefaultRequestHeaders.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));

            var appSettings = Resolver.Resolve<IAppSettingsService>();
            if(!string.IsNullOrWhiteSpace(appSettings.BaseUrl))
            {
                BaseAddress = new Uri($"{appSettings.BaseUrl}/identity");
            }
            else if(!string.IsNullOrWhiteSpace(appSettings.IdentityUrl))
            {
                BaseAddress = new Uri($"{appSettings.IdentityUrl}");
            }
            else
            {
                //BaseAddress = new Uri("http://169.254.80.80:33656"); // Desktop from VS Android Emulator
                //BaseAddress = new Uri("http://192.168.1.3:33656"); // Desktop
                //BaseAddress = new Uri("https://preview-identity.bitwarden.com"); // Preview
                BaseAddress = new Uri("https://identity.bitwarden.com"); // Production
            }
        }
    }
}
