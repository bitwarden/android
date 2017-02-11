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
    public class LoginService : ILoginService
    {
        private readonly ILoginRepository _loginRepository;
        private readonly IAuthService _authService;
        private readonly ILoginApiRepository _loginApiRepository;
        private readonly ISettingsService _settingsService;

        public LoginService(
            ILoginRepository loginRepository,
            IAuthService authService,
            ILoginApiRepository loginApiRepository,
            ISettingsService settingsService)
        {
            _loginRepository = loginRepository;
            _authService = authService;
            _loginApiRepository = loginApiRepository;
            _settingsService = settingsService;
        }

        public async Task<Login> GetByIdAsync(string id)
        {
            var data = await _loginRepository.GetByIdAsync(id);
            if(data == null || data.UserId != _authService.UserId)
            {
                return null;
            }

            var login = new Login(data);
            return login;
        }

        public async Task<IEnumerable<Login>> GetAllAsync()
        {
            var data = await _loginRepository.GetAllByUserIdAsync(_authService.UserId);
            var logins = data.Select(f => new Login(f));
            return logins;
        }

        public async Task<IEnumerable<Login>> GetAllAsync(bool favorites)
        {
            var data = await _loginRepository.GetAllByUserIdAsync(_authService.UserId, favorites);
            var logins = data.Select(f => new Login(f));
            return logins;
        }

        public async Task<IEnumerable<Login>> GetAllAsync(string uriString)
        {
            if(string.IsNullOrWhiteSpace(uriString))
            {
                return new List<Login>();
            }

            Uri uri = null;
            DomainName domainName = null;
            var androidApp = false;

            if(!Uri.TryCreate(uriString, UriKind.Absolute, out uri) || !DomainName.TryParse(uri.Host, out domainName))
            {
                if(domainName == null)
                {
                    androidApp = uriString.StartsWith(Constants.AndroidAppProtocol);
                }
            }


            if(!androidApp && domainName == null)
            {
                return new List<Login>();
            }

            var eqDomains = (await _settingsService.GetEquivalentDomainsAsync()).Select(d => d.ToArray());
            var matchingDomains = eqDomains
                .Where(d => (androidApp && Array.IndexOf(d, uriString) >= 0) ||
                    (!androidApp && Array.IndexOf(d, domainName.BaseDomain) >= 0))
                .SelectMany(d => d).ToList();
            if(!matchingDomains.Any())
            {
                matchingDomains.Add(androidApp ? uriString : domainName.BaseDomain);
            }

            var matchingDomainsArray = matchingDomains.ToArray();
            var matchingLogins = new List<Login>();
            var logins = await _loginRepository.GetAllByUserIdAsync(_authService.UserId);
            foreach(var login in logins)
            {
                if(string.IsNullOrWhiteSpace(login.Uri))
                {
                    continue;
                }

                var loginUriString = new CipherString(login.Uri).Decrypt();
                if(string.IsNullOrWhiteSpace(loginUriString))
                {
                    continue;
                }

                if(androidApp && Array.IndexOf(matchingDomainsArray, loginUriString) >= 0)
                {
                    matchingLogins.Add(new Login(login));
                    continue;
                }

                Uri loginUri;
                DomainName loginDomainName;
                if(!Uri.TryCreate(loginUriString, UriKind.Absolute, out loginUri)
                    || !DomainName.TryParse(loginUri.Host, out loginDomainName))
                {
                    continue;
                }

                if(Array.IndexOf(matchingDomainsArray, loginDomainName.BaseDomain) >= 0)
                {
                    matchingLogins.Add(new Login(login));
                }
            }

            return matchingLogins;
        }

        public async Task<ApiResult<LoginResponse>> SaveAsync(Login login)
        {
            ApiResult<LoginResponse> response = null;
            var request = new LoginRequest(login);

            if(login.Id == null)
            {
                response = await _loginApiRepository.PostAsync(request);
            }
            else
            {
                response = await _loginApiRepository.PutAsync(login.Id, request);
            }

            if(response.Succeeded)
            {
                var data = new LoginData(response.Result, _authService.UserId);
                if(login.Id == null)
                {
                    await _loginRepository.InsertAsync(data);
                    login.Id = data.Id;
                }
                else
                {
                    await _loginRepository.UpdateAsync(data);
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
            var response = await _loginApiRepository.DeleteAsync(id);
            if(response.Succeeded)
            {
                await _loginRepository.DeleteAsync(id);
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
