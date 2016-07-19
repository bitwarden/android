using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.App.Models.Api;
using Bit.App.Models.Data;
using Xamarin.Forms;

namespace Bit.App.Services
{
    public class SiteService : ISiteService
    {
        private readonly ISiteRepository _siteRepository;
        private readonly IAuthService _authService;
        private readonly ISiteApiRepository _siteApiRepository;

        public SiteService(
            ISiteRepository siteRepository,
            IAuthService authService,
            ISiteApiRepository siteApiRepository)
        {
            _siteRepository = siteRepository;
            _authService = authService;
            _siteApiRepository = siteApiRepository;
        }

        public async Task<Site> GetByIdAsync(string id)
        {
            var data = await _siteRepository.GetByIdAsync(id);
            if(data == null || data.UserId != _authService.UserId)
            {
                return null;
            }

            var site = new Site(data);
            return site;
        }

        public async Task<IEnumerable<Site>> GetAllAsync()
        {
            var data = await _siteRepository.GetAllByUserIdAsync(_authService.UserId);
            var sites = data.Select(f => new Site(f));
            return sites;
        }

        public async Task<IEnumerable<Site>> GetAllAsync(bool favorites)
        {
            var data = await _siteRepository.GetAllByUserIdAsync(_authService.UserId, favorites);
            var sites = data.Select(f => new Site(f));
            return sites;
        }

        public async Task<ApiResult<SiteResponse>> SaveAsync(Site site)
        {
            ApiResult<SiteResponse> response = null;
            var request = new SiteRequest(site);

            if(site.Id == null)
            {
                response = await _siteApiRepository.PostAsync(request);
            }
            else
            {
                response = await _siteApiRepository.PutAsync(site.Id, request);
            }

            if(response.Succeeded)
            {
                var data = new SiteData(response.Result, _authService.UserId);
                if(site.Id == null)
                {
                    await _siteRepository.InsertAsync(data);
                    site.Id = data.Id;
                }
                else
                {
                    await _siteRepository.UpdateAsync(data);
                }
            }
            else if(response.StatusCode == System.Net.HttpStatusCode.Forbidden 
                || response.StatusCode == System.Net.HttpStatusCode.Unauthorized)
            {
                MessagingCenter.Send(Application.Current, "Logout", (string)null);
            }

            return response;
        }

        public async Task<ApiResult> DeleteAsync(string id)
        {
            var response = await _siteApiRepository.DeleteAsync(id);
            if(response.Succeeded)
            {
                await _siteRepository.DeleteAsync(id);
            }
            else if(response.StatusCode == System.Net.HttpStatusCode.Forbidden
                || response.StatusCode == System.Net.HttpStatusCode.Unauthorized)
            {
                MessagingCenter.Send(Application.Current, "Logout", (string)null);
            }

            return response;
        }
    }
}
