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
    public class CipherService : ICipherService
    {
        private readonly string[] _ignoredSearchTerms = new string[] { "com", "net", "org", "android",
            "io", "co", "uk", "au", "nz", "fr", "de", "tv", "info", "app", "apps", "eu", "me", "dev", "jp", "mobile" };
        private readonly ICipherRepository _cipherRepository;
        private readonly IAttachmentRepository _attachmentRepository;
        private readonly IAuthService _authService;
        private readonly ICipherApiRepository _cipherApiRepository;
        private readonly ISettingsService _settingsService;
        private readonly ICryptoService _cryptoService;

        private List<Cipher> _cachedCiphers = null;

        public CipherService(
            ICipherRepository cipherRepository,
            IAttachmentRepository attachmentRepository,
            IAuthService authService,
            ICipherApiRepository cipherApiRepository,
            ISettingsService settingsService,
            ICryptoService cryptoService)
        {
            _cipherRepository = cipherRepository;
            _attachmentRepository = attachmentRepository;
            _authService = authService;
            _cipherApiRepository = cipherApiRepository;
            _settingsService = settingsService;
            _cryptoService = cryptoService;
        }

        public async Task<Cipher> GetByIdAsync(string id)
        {
            var data = await _cipherRepository.GetByIdAsync(id);
            if(data == null || data.UserId != _authService.UserId)
            {
                return null;
            }

            var attachments = await _attachmentRepository.GetAllByCipherIdAsync(id);
            var cipher = new Cipher(data, attachments);
            return cipher;
        }

        public async Task<IEnumerable<Cipher>> GetAllAsync()
        {
            if(_cachedCiphers != null)
            {
                return _cachedCiphers;
            }

            var attachmentData = await _attachmentRepository.GetAllByUserIdAsync(_authService.UserId);
            var attachmentDict = attachmentData.GroupBy(a => a.LoginId).ToDictionary(g => g.Key, g => g.ToList());
            var data = await _cipherRepository.GetAllByUserIdAsync(_authService.UserId);
            _cachedCiphers = data
                .Select(f => new Cipher(f, attachmentDict.ContainsKey(f.Id) ? attachmentDict[f.Id] : null))
                .ToList();
            return _cachedCiphers;
        }

        public async Task<IEnumerable<Cipher>> GetAllAsync(bool favorites)
        {
            var attachmentData = await _attachmentRepository.GetAllByUserIdAsync(_authService.UserId);
            var attachmentDict = attachmentData.GroupBy(a => a.LoginId).ToDictionary(g => g.Key, g => g.ToList());
            var data = await _cipherRepository.GetAllByUserIdAsync(_authService.UserId, favorites);
            var cipher = data.Select(f => new Cipher(f, attachmentDict.ContainsKey(f.Id) ? attachmentDict[f.Id] : null));
            return cipher;
        }

        public async Task<Tuple<IEnumerable<Cipher>, IEnumerable<Cipher>>> GetAllAsync(string uriString)
        {
            if(string.IsNullOrWhiteSpace(uriString))
            {
                return null;
            }

            string domainName = null;
            var mobileApp = UriIsMobileApp(uriString);

            if(!mobileApp &&
                (!Uri.TryCreate(uriString, UriKind.Absolute, out Uri uri) ||
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
            var matchingLogins = new List<Cipher>();
            var matchingFuzzyLogins = new List<Cipher>();
            var ciphers = await GetAllAsync();
            foreach(var cipher in ciphers)
            {
                if(cipher.Type != Enums.CipherType.Login || cipher.Login?.Uri == null)
                {
                    continue;
                }

                var loginUriString = cipher.Login.Uri.Decrypt(cipher.OrganizationId);
                if(string.IsNullOrWhiteSpace(loginUriString))
                {
                    continue;
                }

                if(Array.IndexOf(matchingDomainsArray, loginUriString) >= 0)
                {
                    matchingLogins.Add(cipher);
                    continue;
                }
                else if(mobileApp && Array.IndexOf(matchingFuzzyDomainsArray, loginUriString) >= 0)
                {
                    matchingFuzzyLogins.Add(cipher);
                    continue;
                }
                else if(!mobileApp)
                {
                    var info = InfoFromMobileAppUri(loginUriString);
                    if(info?.Item1 != null && Array.IndexOf(matchingDomainsArray, info.Item1) >= 0)
                    {
                        matchingFuzzyLogins.Add(cipher);
                        continue;
                    }
                }

                string loginDomainName = null;
                if(Uri.TryCreate(loginUriString, UriKind.Absolute, out Uri loginUri)
                    && DomainName.TryParseBaseDomain(loginUri.Host, out loginDomainName))
                {
                    loginDomainName = loginDomainName.ToLowerInvariant();

                    if(Array.IndexOf(matchingDomainsArray, loginDomainName) >= 0)
                    {
                        matchingLogins.Add(cipher);
                        continue;
                    }
                    else if(mobileApp && Array.IndexOf(matchingFuzzyDomainsArray, loginDomainName) >= 0)
                    {
                        matchingFuzzyLogins.Add(cipher);
                        continue;
                    }
                }

                if(mobileApp && mobileAppSearchTerms != null && mobileAppSearchTerms.Length > 0)
                {
                    var addedFromSearchTerm = false;
                    var loginNameString = cipher.Name == null ? null : 
                        cipher.Name.Decrypt(cipher.OrganizationId)?.ToLowerInvariant();
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
                            matchingFuzzyLogins.Add(cipher);
                            break;
                        }
                    }

                    if(addedFromSearchTerm)
                    {
                        continue;
                    }
                }
            }

            return new Tuple<IEnumerable<Cipher>, IEnumerable<Cipher>>(matchingLogins, matchingFuzzyLogins);
        }

        public async Task<ApiResult<CipherResponse>> SaveAsync(Cipher cipher)
        {
            ApiResult<CipherResponse> response = null;
            var request = new CipherRequest(cipher);

            if(cipher.Id == null)
            {
                response = await _cipherApiRepository.PostAsync(request);
            }
            else
            {
                response = await _cipherApiRepository.PutAsync(cipher.Id, request);
            }

            if(response.Succeeded)
            {
                var data = new CipherData(response.Result, _authService.UserId);
                if(cipher.Id == null)
                {
                    await _cipherRepository.InsertAsync(data);
                    cipher.Id = data.Id;
                }
                else
                {
                    await _cipherRepository.UpdateAsync(data);
                }

                _cachedCiphers = null;
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
            var response = await _cipherApiRepository.DeleteAsync(id);
            if(response.Succeeded)
            {
                await _cipherRepository.DeleteAsync(id);
                _cachedCiphers = null;
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

        public async Task<ApiResult<CipherResponse>> EncryptAndSaveAttachmentAsync(Cipher cipher, byte[] data, string fileName)
        {
            var encFileName = fileName.Encrypt(cipher.OrganizationId);
            var encBytes = _cryptoService.EncryptToBytes(data,
                cipher.OrganizationId != null ? _cryptoService.GetOrgKey(cipher.OrganizationId) : null);
            var response = await _cipherApiRepository.PostAttachmentAsync(cipher.Id, encBytes, encFileName.EncryptedString);

            if(response.Succeeded)
            {
                var attachmentData = response.Result.Attachments.Select(a => new AttachmentData(a, cipher.Id));
                foreach(var attachment in attachmentData)
                {
                    await _attachmentRepository.UpsertAsync(attachment);
                }
                cipher.Attachments = response.Result.Attachments.Select(a => new Attachment(a));
            }
            else if(response.StatusCode == System.Net.HttpStatusCode.Forbidden
                || response.StatusCode == System.Net.HttpStatusCode.Unauthorized)
            {
                MessagingCenter.Send(Application.Current, "Logout", (string)null);
            }

            return response;
        }

        public async Task<ApiResult> DeleteAttachmentAsync(Cipher cipher, string attachmentId)
        {
            var response = await _cipherApiRepository.DeleteAttachmentAsync(cipher.Id, attachmentId);
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
