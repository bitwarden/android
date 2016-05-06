using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models.Api;
using Newtonsoft.Json;

namespace Bit.App.Repositories
{
    public class SiteApiRepository : ApiRepository<SiteRequest, SiteResponse, string>, ISiteApiRepository
    {
        protected override string ApiRoute => "sites";

        public virtual async Task<ApiResult<ListResponse<SiteResponse>>> GetByRevisionDateAsync(DateTime since)
        {
            var requestMessage = new TokenHttpRequestMessage()
            {
                Method = HttpMethod.Get,
                RequestUri = new Uri(Client.BaseAddress, string.Concat(ApiRoute, "?since=", since)),
            };

            var response = await Client.SendAsync(requestMessage);
            if(!response.IsSuccessStatusCode)
            {
                return await HandleErrorAsync<ListResponse<SiteResponse>>(response);
            }

            var responseContent = await response.Content.ReadAsStringAsync();
            var responseObj = JsonConvert.DeserializeObject<ListResponse<SiteResponse>>(responseContent);
            return ApiResult<ListResponse<SiteResponse>>.Success(responseObj, response.StatusCode);
        }
    }
}
