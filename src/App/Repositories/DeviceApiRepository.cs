using System;
using System.Net.Http;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models.Api;
using Newtonsoft.Json;
using Plugin.Connectivity.Abstractions;
using System.Net;

namespace Bit.App.Repositories
{
    public class DeviceApiRepository : ApiRepository<DeviceRequest, DeviceResponse, string>, IDeviceApiRepository
    {
        public DeviceApiRepository(
            IConnectivity connectivity,
            IHttpService httpService)
            : base(connectivity, httpService)
        { }

        protected override string ApiRoute => "devices";

        public virtual async Task<ApiResult> PutTokenAsync(string identifier, DeviceTokenRequest request)
        {
            if(!Connectivity.IsConnected)
            {
                return HandledNotConnected();
            }

            using(var client = HttpService.Client)
            {
                var requestMessage = new TokenHttpRequestMessage(request)
                {
                    Method = HttpMethod.Put,
                    RequestUri = new Uri(client.BaseAddress, string.Concat(ApiRoute, "/identifier/", identifier, "/token")),
                };

                try
                {
                    var response = await client.SendAsync(requestMessage).ConfigureAwait(false);
                    if(!response.IsSuccessStatusCode)
                    {
                        return await HandleErrorAsync(response).ConfigureAwait(false);
                    }

                    return ApiResult.Success(response.StatusCode);
                }
                catch(WebException)
                {
                    return HandledWebException();
                }
            }
        }

        public virtual async Task<ApiResult> PutClearTokenAsync(string identifier)
        {
            if(!Connectivity.IsConnected)
            {
                return HandledNotConnected();
            }

            using(var client = HttpService.Client)
            {
                var requestMessage = new TokenHttpRequestMessage
                {
                    Method = HttpMethod.Put,
                    RequestUri = new Uri(client.BaseAddress,
                        string.Concat(ApiRoute, "/identifier/", identifier, "/clear-token"))
                };

                try
                {
                    var response = await client.SendAsync(requestMessage).ConfigureAwait(false);
                    if(!response.IsSuccessStatusCode)
                    {
                        return await HandleErrorAsync(response).ConfigureAwait(false);
                    }

                    return ApiResult.Success(response.StatusCode);
                }
                catch(WebException)
                {
                    return HandledWebException();
                }
            }
        }
    }
}
