using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
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
            var sites = data.Select(s => new Site(s));
            return Task.FromResult(sites);
        }

        public async Task<ApiResult<SiteResponse>> SaveAsync(Site site)
        {
            var request = new SiteRequest(site);
            var requestMessage = new TokenHttpRequestMessage(request)
            {
                Method = site.Id == null ? HttpMethod.Post : HttpMethod.Put,
                RequestUri = new Uri(_apiService.Client.BaseAddress, site.Id == null ? "/sites" : $"/folders/{site.Id}")
            };

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
                await base.CreateAsync(data);
                site.Id = responseObj.Id;
            }
            else
            {
                await base.ReplaceAsync(data);
            }

            return ApiResult<SiteResponse>.Success(responseObj, response.StatusCode);
        }

        public new async Task<ApiResult<object>> DeleteAsync(string id)
        {
            var requestMessage = new TokenHttpRequestMessage
            {
                Method = HttpMethod.Delete,
                RequestUri = new Uri(_apiService.Client.BaseAddress, $"/sites/{id}")
            };

            var response = await _apiService.Client.SendAsync(requestMessage);
            if(!response.IsSuccessStatusCode)
            {
                return await _apiService.HandleErrorAsync<object>(response);
            }

            await base.DeleteAsync(id);
            return ApiResult<object>.Success(null, response.StatusCode);
        }
    }
}
