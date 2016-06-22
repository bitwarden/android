using System;
using System.Net.Http;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models.Api;
using Newtonsoft.Json;

namespace Bit.App.Repositories
{
    public class DeviceApiRepository : ApiRepository<DeviceRequest, DeviceResponse, string>, IDeviceApiRepository
    {
        protected override string ApiRoute => "devices";

        public virtual async Task<ApiResult<DeviceResponse>> PutTokenAsync(string identifier, DeviceTokenRequest request)
        {
            var requestMessage = new TokenHttpRequestMessage(request)
            {
                Method = HttpMethod.Put,
                RequestUri = new Uri(Client.BaseAddress, string.Concat(ApiRoute, "/identifier/", identifier, "/token")),
            };

            var response = await Client.SendAsync(requestMessage);
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
