using System.Net.Http;
using System;
using System.Net.Http.Headers;
using XLabs.Ioc;
using Bit.App.Abstractions;

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
            DefaultRequestHeaders.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));

            var appSettings = Resolver.Resolve<IAppSettingsService>();
            if(!string.IsNullOrWhiteSpace(appSettings.BaseUrl))
            {
                BaseAddress = new Uri($"{appSettings.BaseUrl}/api");
            }
            else if(!string.IsNullOrWhiteSpace(appSettings.ApiUrl))
            {
                BaseAddress = new Uri($"{appSettings.ApiUrl}");
            }
            else
            {
                //BaseAddress = new Uri("http://169.254.80.80:4000"); // Desktop from VS Android Emulator
                //BaseAddress = new Uri("http://192.168.1.3:4000"); // Desktop
                //BaseAddress = new Uri("https://preview-api.bitwarden.com"); // Preview
                BaseAddress = new Uri("https://api.bitwarden.com"); // Production
            }
        }
    }
}
