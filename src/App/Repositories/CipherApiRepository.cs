using System;
using System.Net.Http;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models.Api;
using Newtonsoft.Json;
using Plugin.Connectivity.Abstractions;
using System.IO;

namespace Bit.App.Repositories
{
    public class CipherApiRepository : ApiRepository<CipherRequest, CipherResponse, string>, ICipherApiRepository
    {
        public CipherApiRepository(
            IConnectivity connectivity,
            IHttpService httpService,
            ITokenService tokenService)
            : base(connectivity, httpService, tokenService)
        { }

        protected override string ApiRoute => "/ciphers";

        public virtual async Task<ApiResult<CipherResponse>> PostAttachmentAsync(string cipherId, byte[] data,
            string key, string fileName)
        {
            if(!Connectivity.IsConnected)
            {
                return HandledNotConnected<CipherResponse>();
            }

            var tokenStateResponse = await HandleTokenStateAsync<CipherResponse>();
            if(!tokenStateResponse.Succeeded)
            {
                return tokenStateResponse;
            }

            using(var client = HttpService.ApiClient)
            using(var content = new MultipartFormDataContent("--BWMobileFormBoundary" + DateTime.UtcNow.Ticks))
            {
                content.Add(new StringContent(key), "key");
                content.Add(new StreamContent(new MemoryStream(data)), "data", fileName);

                var requestMessage = new TokenHttpRequestMessage
                {
                    Method = HttpMethod.Post,
                    RequestUri = new Uri(string.Concat(client.BaseAddress, ApiRoute, "/", cipherId, "/attachment")),
                    Content = content
                };

                try
                {
                    var response = await client.SendAsync(requestMessage).ConfigureAwait(false);
                    if(!response.IsSuccessStatusCode)
                    {
                        return await HandleErrorAsync<CipherResponse>(response).ConfigureAwait(false);
                    }

                    var responseContent = await response.Content.ReadAsStringAsync().ConfigureAwait(false);
                    var responseObj = JsonConvert.DeserializeObject<CipherResponse>(responseContent);
                    return ApiResult<CipherResponse>.Success(responseObj, response.StatusCode);
                }
                catch
                {
                    return HandledWebException<CipherResponse>();
                }
            }
        }

        public virtual async Task<ApiResult> DeleteAttachmentAsync(string cipherId, string attachmentId)
        {
            if(!Connectivity.IsConnected)
            {
                return HandledNotConnected();
            }

            var tokenStateResponse = await HandleTokenStateAsync();
            if(!tokenStateResponse.Succeeded)
            {
                return tokenStateResponse;
            }

            using(var client = HttpService.ApiClient)
            {
                var requestMessage = new TokenHttpRequestMessage()
                {
                    Method = HttpMethod.Delete,
                    RequestUri = new Uri(
                        string.Concat(client.BaseAddress, ApiRoute, "/", cipherId, "/attachment/", attachmentId)),
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
                catch
                {
                    return HandledWebException();
                }
            }
        }
    }
}
