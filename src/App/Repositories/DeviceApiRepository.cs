using System;
using System.Net.Http;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models.Api;
using Newtonsoft.Json;
using Plugin.Connectivity.Abstractions;

namespace Bit.App.Repositories
{
    public class DeviceApiRepository : ApiRepository<DeviceRequest, DeviceResponse, string>, IDeviceApiRepository
    {
        public DeviceApiRepository(IConnectivity connectivity)
            : base(connectivity)
        { }

        protected override string ApiRoute => "devices";

        public virtual async Task<ApiResult<DeviceResponse>> PutTokenAsync(string identifier, DeviceTokenRequest request)
        {
            if(!Connectivity.IsConnected)
            {
                return HandledNotConnected<DeviceResponse>();
            }

            using(var client = new ApiHttpClient())
            {
                var requestMessage = new TokenHttpRequestMessage(request)
                {
                    Method = HttpMethod.Put,
                    RequestUri = new Uri(client.BaseAddress, string.Concat(ApiRoute, "/identifier/", identifier, "/token")),
                };

                var response = await client.SendAsync(requestMessage);
                if(!response.IsSuccessStatusCode)
                {
                    return await HandleErrorAsync<DeviceResponse>(response);
                }

                var responseContent = await response.Content.ReadAsStringAsync();
                var responseObj = JsonConvert.DeserializeObject<DeviceResponse>(responseContent);
                return ApiResult<DeviceResponse>.Success(responseObj, response.StatusCode);
            }
        }

        public virtual async Task<ApiResult<DeviceResponse>> PutClearTokenAsync(string identifier)
        {
            if(!Connectivity.IsConnected)
            {
                return HandledNotConnected<DeviceResponse>();
            }

            using(var client = new ApiHttpClient())
            {
                var requestMessage = new TokenHttpRequestMessage
                {
                    Method = HttpMethod.Put,
                    RequestUri = new Uri(client.BaseAddress, string.Concat(ApiRoute, "/identifier/", identifier, "/clear-token")),
                };

                var response = await client.SendAsync(requestMessage);
                if(!response.IsSuccessStatusCode)
                {
                    return await HandleErrorAsync<DeviceResponse>(response);
                }

                var responseContent = await response.Content.ReadAsStringAsync();
                var responseObj = JsonConvert.DeserializeObject<DeviceResponse>(responseContent);
                return ApiResult<DeviceResponse>.Success(responseObj, response.StatusCode);
            }
        }
    }
}
