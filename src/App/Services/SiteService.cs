using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.App.Models.Api;
using Bit.App.Models.Data;
using Newtonsoft.Json;

namespace Bit.App.Services
{
    public class SiteService : Repository<SiteData, string>, ISiteService
    {
        private readonly IAuthService _authService;
        private readonly IApiService _apiService;

        public SiteService(
            ISqlService sqlService,
            IAuthService authService,
            IApiService apiService)
            : base(sqlService)
        {
            _authService = authService;
            _apiService = apiService;
        }

        public new Task<IEnumerable<Site>> GetAllAsync()
        {
            var data = Connection.Table<SiteData>().Where(f => f.UserId == _authService.UserId).Cast<SiteData>();
            return Task.FromResult(data.Select(s => new Site(s)));
        }

        public async Task<ApiResult<SiteResponse>> SaveAsync(Site site)
        {
            var request = new SiteRequest(site);
            var requestContent = JsonConvert.SerializeObject(request);
            var requestMessage = new HttpRequestMessage
            {
                Method = site.Id == null ? HttpMethod.Post : HttpMethod.Put,
                RequestUri = new Uri(_apiService.Client.BaseAddress, site.Id == null ? "/sites" : string.Concat("/folders/", site.Id)),
                Content = new StringContent(requestContent, Encoding.UTF8, "application/json")
            };
            requestMessage.Headers.Add("Authorization", string.Concat("Bearer ", _authService.Token));

            var response = await _apiService.Client.SendAsync(requestMessage);
            if(!response.IsSuccessStatusCode)
            {
                return await _apiService.HandleErrorAsync<SiteResponse>(response);
            }

            var responseContent = await response.Content.ReadAsStringAsync();
            var responseObj = JsonConvert.DeserializeObject<SiteResponse>(responseContent);
            var data = new SiteData(responseObj, _authService.UserId);

            if(site.Id == null)
            {
                await CreateAsync(data);
                site.Id = responseObj.Id;
            }
            else
            {
                await ReplaceAsync(data);
            }

            return ApiResult<SiteResponse>.Success(responseObj, response.StatusCode);
        }
    }
}
