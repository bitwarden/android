using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.App.Models.Api;
using Bit.App.Models.Data;
using Xamarin.Forms;
using System.Net.Http;

namespace Bit.App.Services
{
    public class LoginService : ILoginService
    {
        private readonly string[] _ignoredSearchTerms = new string[] { "com", "net", "org", "android",
            "io", "co", "uk", "au", "nz", "fr", "de", "tv", "info", "app", "apps", "eu", "me", "dev", "jp", "mobile" };
        private readonly ILoginRepository _loginRepository;
        private readonly IAttachmentRepository _attachmentRepository;
        private readonly IAuthService _authService;
        private readonly ILoginApiRepository _loginApiRepository;
        private readonly ICipherApiRepository _cipherApiRepository;
        private readonly ISettingsService _settingsService;
        private readonly ICryptoService _cryptoService;

        public LoginService(
            ILoginRepository loginRepository,
            IAttachmentRepository attachmentRepository,
            IAuthService authService,
            ILoginApiRepository loginApiRepository,
            ICipherApiRepository cipherApiRepository,
            ISettingsService settingsService,
            ICryptoService cryptoService)
        {
            _loginRepository = loginRepository;
            _attachmentRepository = attachmentRepository;
            _authService = authService;
            _loginApiRepository = loginApiRepository;
            _cipherApiRepository = cipherApiRepository;
            _settingsService = settingsService;
            _cryptoService = cryptoService;
        }

        public async Task<Login> GetByIdAsync(string id)
        {
            var data = await _loginRepository.GetByIdAsync(id);
            if(data == null || data.UserId != _authService.UserId)
            {
                return null;
            }

            var attachments = await _attachmentRepository.GetAllByLoginIdAsync(id);
            var login = new Login(data, attachments);
            return login;
        }

        public async Task<IEnumerable<Login>> GetAllAsync()
        {
            var attachmentData = await _attachmentRepository.GetAllByUserIdAsync(_authService.UserId);
            var attachmentDict = attachmentData.GroupBy(a => a.LoginId).ToDictionary(g => g.Key, g => g.ToList());
            var data = await _loginRepository.GetAllByUserIdAsync(_authService.UserId);
            var logins = data.Select(f => new Login(f, attachmentDict.ContainsKey(f.Id) ? attachmentDict[f.Id] : null));
            return logins;
        }

        public async Task<IEnumerable<Login>> GetAllAsync(bool favorites)
        {
            var attachmentData = await _attachmentRepository.GetAllByUserIdAsync(_authService.UserId);
            var attachmentDict = attachmentData.GroupBy(a => a.LoginId).ToDictionary(g => g.Key, g => g.ToList());
            var data = await _loginRepository.GetAllByUserIdAsync(_authService.UserId, favorites);
            var logins = data.Select(f => new Login(f, attachmentDict.ContainsKey(f.Id) ? attachmentDict[f.Id] : null));
            return logins;
        }

        public async Task<Tuple<IEnumerable<Login>, IEnumerable<Login>>> GetAllAsync(string uriString)
        {
            if(string.IsNullOrWhiteSpace(uriString))
            {
                return null;
            }

            Uri uri = null;
            string domainName = null;
            var mobileApp = UriIsMobileApp(uriString);

            if(!mobileApp &&
                (!Uri.TryCreate(uriString, UriKind.Absolute, out uri) ||
                    !DomainName.TryParseBaseDomain(uri.Host, out domainName)))
            {
                return null;
            }

            var mobileAppInfo = InfoFromMobileAppUri(uriString);
            var mobileAppWebUriString = mobileAppInfo?.Item1;
            var mobileAppSearchTerms = mobileAppInfo?.Item2;
            var eqDomains = (await _settingsService.GetEquivalentDomainsAsync()).Select(d => d.ToArray());
            var matchingDomains = new List<string>();
            var matchingFuzzyDomains = new List<string>();
            foreach(var eqDomain in eqDomains)
            {
                if(mobileApp)
                {
                    if(Array.IndexOf(eqDomain, uriString) >= 0)
                    {
                        matchingDomains.AddRange(eqDomain.Select(d => d).ToList());
                    }
                    else if(mobileAppWebUriString != null && Array.IndexOf(eqDomain, mobileAppWebUriString) >= 0)
                    {
                        matchingFuzzyDomains.AddRange(eqDomain.Select(d => d).ToList());
                    }
                }
                else if(Array.IndexOf(eqDomain, domainName) >= 0)
                {
                    matchingDomains.AddRange(eqDomain.Select(d => d).ToList());
                }
            }

            if(!matchingDomains.Any())
            {
                matchingDomains.Add(mobileApp ? uriString : domainName);
            }

            if(mobileApp && mobileAppWebUriString != null &&
                !matchingFuzzyDomains.Any() && !matchingDomains.Contains(mobileAppWebUriString))
            {
                matchingFuzzyDomains.Add(mobileAppWebUriString);
            }

            var matchingDomainsArray = matchingDomains.ToArray();
            var matchingFuzzyDomainsArray = matchingFuzzyDomains.ToArray();
            var matchingLogins = new List<Login>();
            var matchingFuzzyLogins = new List<Login>();
            var logins = await _loginRepository.GetAllByUserIdAsync(_authService.UserId);
            foreach(var login in logins)
            {
                if(string.IsNullOrWhiteSpace(login.Uri))
                {
                    continue;
                }

                var loginUriString = new CipherString(login.Uri).Decrypt(login.OrganizationId);
                if(string.IsNullOrWhiteSpace(loginUriString))
                {
                    continue;
                }

                if(Array.IndexOf(matchingDomainsArray, loginUriString) >= 0)
                {
                    matchingLogins.Add(new Login(login));
                    continue;
                }
                else if(mobileApp && Array.IndexOf(matchingFuzzyDomainsArray, loginUriString) >= 0)
                {
                    matchingFuzzyLogins.Add(new Login(login));
                    continue;
                }
                else if(!mobileApp)
                {
                    var info = InfoFromMobileAppUri(loginUriString);
                    if(info?.Item1 != null && Array.IndexOf(matchingDomainsArray, info.Item1) >= 0)
                    {
                        matchingFuzzyLogins.Add(new Login(login));
                        continue;
                    }
                }

                Uri loginUri;
                string loginDomainName = null;
                if(Uri.TryCreate(loginUriString, UriKind.Absolute, out loginUri)
                    && DomainName.TryParseBaseDomain(loginUri.Host, out loginDomainName))
                {
                    loginDomainName = loginDomainName.ToLowerInvariant();

                    if(Array.IndexOf(matchingDomainsArray, loginDomainName) >= 0)
                    {
                        matchingLogins.Add(new Login(login));
                        continue;
                    }
                    else if(mobileApp && Array.IndexOf(matchingFuzzyDomainsArray, loginDomainName) >= 0)
                    {
                        matchingFuzzyLogins.Add(new Login(login));
                        continue;
                    }
                }

                if(mobileApp && mobileAppSearchTerms != null && mobileAppSearchTerms.Length > 0)
                {
                    var addedFromSearchTerm = false;
                    var loginNameString = login.Name == null ? null :
                        new CipherString(login.Name).Decrypt(login.OrganizationId)?.ToLowerInvariant();
                    foreach(var term in mobileAppSearchTerms)
                    {
                        addedFromSearchTerm = (loginDomainName != null && loginDomainName.Contains(term)) ||
                            (loginNameString != null && loginNameString.Contains(term));
                        if(!addedFromSearchTerm)
                        {
                            addedFromSearchTerm = (loginDomainName != null && term.Contains(loginDomainName.Split('.')[0]))
                                || (loginNameString != null && term.Contains(loginNameString));
                        }

                        if(addedFromSearchTerm)
                        {
                            matchingFuzzyLogins.Add(new Login(login));
                            break;
                        }
                    }

                    if(addedFromSearchTerm)
                    {
                        continue;
                    }
                }
            }

            return new Tuple<IEnumerable<Login>, IEnumerable<Login>>(matchingLogins, matchingFuzzyLogins);
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

        public async Task<byte[]> DownloadAndDecryptAttachmentAsync(string url, string orgId = null)
        {
            using(var client = new HttpClient())
            {
                try
                {
                    var response = await client.GetAsync(new Uri(url)).ConfigureAwait(false);
                    if(!response.IsSuccessStatusCode)
                    {
                        return null;
                    }

                    var data = await response.Content.ReadAsByteArrayAsync();
                    if(data == null)
                    {
                        return null;
                    }

                    if(!string.IsNullOrWhiteSpace(orgId))
                    {
                        return _cryptoService.DecryptToBytes(data, _cryptoService.GetOrgKey(orgId));
                    }
                    else
                    {
                        return _cryptoService.DecryptToBytes(data, null);
                    }
                }
                catch
                {
                    return null;
                }
            }
        }

        public async Task<ApiResult<CipherResponse>> EncryptAndSaveAttachmentAsync(Login login, byte[] data, string fileName)
        {
            var encFileName = fileName.Encrypt(login.OrganizationId);
            var encBytes = _cryptoService.EncryptToBytes(data,
                login.OrganizationId != null ? _cryptoService.GetOrgKey(login.OrganizationId) : null);
            var response = await _cipherApiRepository.PostAttachmentAsync(login.Id, encBytes, encFileName.EncryptedString);

            if(response.Succeeded)
            {
                var attachmentData = response.Result.Attachments.Select(a => new AttachmentData(a, login.Id));
                foreach(var attachment in attachmentData)
                {
                    await _attachmentRepository.UpsertAsync(attachment);
                }
                login.Attachments = response.Result.Attachments.Select(a => new Attachment(a));
            }
            else if(response.StatusCode == System.Net.HttpStatusCode.Forbidden
                || response.StatusCode == System.Net.HttpStatusCode.Unauthorized)
            {
                MessagingCenter.Send(Application.Current, "Logout", (string)null);
            }

            return response;
        }

        public async Task<ApiResult> DeleteAttachmentAsync(Login login, string attachmentId)
        {
            var response = await _cipherApiRepository.DeleteAttachmentAsync(login.Id, attachmentId);
            if(response.Succeeded)
            {
                await _attachmentRepository.DeleteAsync(attachmentId);
            }
            else if(response.StatusCode == System.Net.HttpStatusCode.Forbidden
                || response.StatusCode == System.Net.HttpStatusCode.Unauthorized)
            {
                MessagingCenter.Send(Application.Current, "Logout", (string)null);
            }

            return response;
        }

        private Tuple<string, string[]> InfoFromMobileAppUri(string mobileAppUriString)
        {
            if(UriIsAndroidApp(mobileAppUriString))
            {
                return InfoFromAndroidAppUri(mobileAppUriString);
            }
            else if(UriIsiOSApp(mobileAppUriString))
            {
                return InfoFromiOSAppUri(mobileAppUriString);
            }

            return null;
        }

        private Tuple<string, string[]> InfoFromAndroidAppUri(string androidAppUriString)
        {
            if(!UriIsAndroidApp(androidAppUriString))
            {
                return null;
            }

            var androidUriParts = androidAppUriString.Replace(Constants.AndroidAppProtocol, string.Empty).Split('.');
            if(androidUriParts.Length >= 2)
            {
                var webUri = string.Join(".", androidUriParts[1], androidUriParts[0]);
                var searchTerms = androidUriParts.Where(p => !_ignoredSearchTerms.Contains(p))
                    .Select(p => p.ToLowerInvariant()).ToArray();
                return new Tuple<string, string[]>(webUri, searchTerms);
            }

            return null;
        }

        private Tuple<string, string[]> InfoFromiOSAppUri(string iosAppUriString)
        {
            if(!UriIsiOSApp(iosAppUriString))
            {
                return null;
            }

            var webUri = iosAppUriString.Replace(Constants.iOSAppProtocol, string.Empty);
            return new Tuple<string, string[]>(webUri, null);
        }

        private bool UriIsMobileApp(string uriString)
        {
            return UriIsAndroidApp(uriString) || UriIsiOSApp(uriString);
        }

        private bool UriIsAndroidApp(string uriString)
        {
            return uriString.StartsWith(Constants.AndroidAppProtocol);
        }

        private bool UriIsiOSApp(string uriString)
        {
            return uriString.StartsWith(Constants.iOSAppProtocol);
        }
    }
}
