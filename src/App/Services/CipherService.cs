using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.App.Models.Api;
using Bit.App.Models.Data;
using System.Net.Http;
using Bit.App.Utilities;
using System.Text.RegularExpressions;
using Xamarin.Forms;

namespace Bit.App.Services
{
    public class CipherService : ICipherService
    {
        public static List<Cipher> CachedCiphers = null;

        private readonly string[] _ignoredSearchTerms = new string[] { "com", "net", "org", "android",
            "io", "co", "uk", "au", "nz", "fr", "de", "tv", "info", "app", "apps", "eu", "me", "dev", "jp", "mobile" };
        private readonly ICipherRepository _cipherRepository;
        private readonly ICipherCollectionRepository _cipherCollectionRepository;
        private readonly IAttachmentRepository _attachmentRepository;
        private readonly IAuthService _authService;
        private readonly ICipherApiRepository _cipherApiRepository;
        private readonly ISettingsService _settingsService;
        private readonly ICryptoService _cryptoService;
        private readonly IAppSettingsService _appSettingsService;

        public CipherService(
            ICipherRepository cipherRepository,
            ICipherCollectionRepository cipherCollectionRepository,
            IAttachmentRepository attachmentRepository,
            IAuthService authService,
            ICipherApiRepository cipherApiRepository,
            ISettingsService settingsService,
            ICryptoService cryptoService,
            IAppSettingsService appSettingsService)
        {
            _cipherRepository = cipherRepository;
            _cipherCollectionRepository = cipherCollectionRepository;
            _attachmentRepository = attachmentRepository;
            _authService = authService;
            _cipherApiRepository = cipherApiRepository;
            _settingsService = settingsService;
            _cryptoService = cryptoService;
            _appSettingsService = appSettingsService;
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
            if(_appSettingsService.ClearCiphersCache)
            {
                CachedCiphers = null;
                _appSettingsService.ClearCiphersCache = false;
            }

            if(CachedCiphers != null)
            {
                return CachedCiphers;
            }

            var attachmentData = await _attachmentRepository.GetAllByUserIdAsync(_authService.UserId);
            var attachmentDict = attachmentData.GroupBy(a => a.LoginId).ToDictionary(g => g.Key, g => g.ToList());
            var data = await _cipherRepository.GetAllByUserIdAsync(_authService.UserId);
            CachedCiphers = data
                .Select(f => new Cipher(f, attachmentDict.ContainsKey(f.Id) ? attachmentDict[f.Id] : null))
                .ToList();
            return CachedCiphers;
        }

        public async Task<IEnumerable<Cipher>> GetAllAsync(bool favorites)
        {
            var ciphers = await GetAllAsync();
            return ciphers.Where(c => c.Favorite == favorites);
        }

        public async Task<IEnumerable<Cipher>> GetAllByFolderAsync(string folderId)
        {
            var ciphers = await GetAllAsync();
            return ciphers.Where(c => c.FolderId == folderId);
        }

        public async Task<IEnumerable<Cipher>> GetAllByCollectionAsync(string collectionId)
        {
            var assoc = await _cipherCollectionRepository.GetAllByUserIdCollectionAsync(_authService.UserId, collectionId);
            var cipherIds = new HashSet<string>(assoc.Select(c => c.CipherId));
            var ciphers = await GetAllAsync();
            return ciphers.Where(c => cipherIds.Contains(c.Id));
        }

        public async Task<Tuple<IEnumerable<Cipher>, IEnumerable<Cipher>, IEnumerable<Cipher>>> GetAllAsync(
            string uriString)
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
            var others = new List<Cipher>();
            var ciphers = await GetAllAsync();
            foreach(var cipher in ciphers)
            {
                if(cipher.Type != Enums.CipherType.Login)
                {
                    others.Add(cipher);
                    continue;
                }

                if(cipher.Login?.Uris == null || !cipher.Login.Uris.Any())
                {
                    continue;
                }

                foreach(var u in cipher.Login.Uris)
                {
                    var loginUriString = u.Uri?.Decrypt(cipher.OrganizationId);
                    if(string.IsNullOrWhiteSpace(loginUriString))
                    {
                        break;
                    }

                    var match = false;
                    switch(u.Match)
                    {
                        case null:
                        case Enums.UriMatchType.Domain:
                            match = CheckDefaultUriMatch(cipher, loginUriString, matchingLogins, matchingFuzzyLogins,
                                matchingDomainsArray, matchingFuzzyDomainsArray, mobileApp, mobileAppSearchTerms);
                            break;
                        case Enums.UriMatchType.Host:
                            var urlHost = Helpers.GetUrlHost(uriString);
                            match = urlHost != null && urlHost == Helpers.GetUrlHost(loginUriString);
                            if(match)
                            {
                                AddMatchingLogin(cipher, matchingLogins, matchingFuzzyLogins);
                            }
                            break;
                        case Enums.UriMatchType.Exact:
                            match = uriString == loginUriString;
                            if(match)
                            {
                                AddMatchingLogin(cipher, matchingLogins, matchingFuzzyLogins);
                            }
                            break;
                        case Enums.UriMatchType.StartsWith:
                            match = uriString.StartsWith(loginUriString);
                            if(match)
                            {
                                AddMatchingLogin(cipher, matchingLogins, matchingFuzzyLogins);
                            }
                            break;
                        case Enums.UriMatchType.RegularExpression:
                            var regex = new Regex(loginUriString, RegexOptions.IgnoreCase, TimeSpan.FromSeconds(1));
                            match = regex.IsMatch(uriString);
                            if(match)
                            {
                                AddMatchingLogin(cipher, matchingLogins, matchingFuzzyLogins);
                            }
                            break;
                        case Enums.UriMatchType.Never:
                        default:
                            break;
                    }

                    if(match)
                    {
                        break;
                    }
                }
            }

            return new Tuple<IEnumerable<Cipher>, IEnumerable<Cipher>, IEnumerable<Cipher>>(
                matchingLogins, matchingFuzzyLogins, others);
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
                await UpsertDataAsync(data, true);
                cipher.Id = data.Id;
            }
            else if(response.StatusCode == System.Net.HttpStatusCode.Forbidden
                || response.StatusCode == System.Net.HttpStatusCode.Unauthorized)
            {
                _authService.LogOut();
            }

            return response;
        }

        public async Task UpsertDataAsync(CipherData cipher, bool sendMessage)
        {
            await _cipherRepository.UpsertAsync(cipher);
            CachedCiphers = null;
            _appSettingsService.ClearCiphersCache = true;
            if(sendMessage)
            {
                MessagingCenter.Send(Application.Current, "UpsertedCipher", cipher.Id);
            }
        }

        public async Task<ApiResult> DeleteAsync(string id)
        {
            var response = await _cipherApiRepository.DeleteAsync(id);
            if(response.Succeeded)
            {
                await DeleteDataAsync(id, true);
            }
            else if(response.StatusCode == System.Net.HttpStatusCode.Forbidden
                || response.StatusCode == System.Net.HttpStatusCode.Unauthorized)
            {
                _authService.LogOut();
            }

            return response;
        }

        public async Task DeleteDataAsync(string id, bool sendMessage)
        {
            if(sendMessage)
            {
                var cipherData = await _cipherRepository.GetByIdAsync(id);
                if(cipherData != null)
                {
                    MessagingCenter.Send(Application.Current, "DeletedCipher", new Cipher(cipherData));
                }
            }
            await _cipherRepository.DeleteAsync(id);
            CachedCiphers = null;
            _appSettingsService.ClearCiphersCache = true;
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
                await UpsertAttachmentDataAsync(attachmentData);
                cipher.Attachments = response.Result.Attachments.Select(a => new Attachment(a));
            }
            else if(response.StatusCode == System.Net.HttpStatusCode.Forbidden
                || response.StatusCode == System.Net.HttpStatusCode.Unauthorized)
            {
                _authService.LogOut();
            }

            return response;
        }

        public async Task UpsertAttachmentDataAsync(IEnumerable<AttachmentData> attachments)
        {
            foreach(var attachment in attachments)
            {
                await _attachmentRepository.UpsertAsync(attachment);
            }
            CachedCiphers = null;
            _appSettingsService.ClearCiphersCache = true;
        }

        public async Task<ApiResult> DeleteAttachmentAsync(Cipher cipher, string attachmentId)
        {
            var response = await _cipherApiRepository.DeleteAttachmentAsync(cipher.Id, attachmentId);
            if(response.Succeeded)
            {
                await DeleteAttachmentDataAsync(attachmentId);
            }
            else if(response.StatusCode == System.Net.HttpStatusCode.Forbidden
                || response.StatusCode == System.Net.HttpStatusCode.Unauthorized)
            {
                _authService.LogOut();
            }

            return response;
        }

        public async Task DeleteAttachmentDataAsync(string attachmentId)
        {
            await _attachmentRepository.DeleteAsync(attachmentId);
            CachedCiphers = null;
            _appSettingsService.ClearCiphersCache = true;
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

        private bool CheckDefaultUriMatch(Cipher cipher, string loginUriString, List<Cipher> matchingLogins,
            List<Cipher> matchingFuzzyLogins, string[] matchingDomainsArray, string[] matchingFuzzyDomainsArray,
            bool mobileApp, string[] mobileAppSearchTerms)
        {
            if(Array.IndexOf(matchingDomainsArray, loginUriString) >= 0)
            {
                AddMatchingLogin(cipher, matchingLogins, matchingFuzzyLogins);
                return true;
            }
            else if(mobileApp && Array.IndexOf(matchingFuzzyDomainsArray, loginUriString) >= 0)
            {
                AddMatchingFuzzyLogin(cipher, matchingLogins, matchingFuzzyLogins);
                return false;
            }
            else if(!mobileApp)
            {
                var info = InfoFromMobileAppUri(loginUriString);
                if(info?.Item1 != null && Array.IndexOf(matchingDomainsArray, info.Item1) >= 0)
                {
                    AddMatchingFuzzyLogin(cipher, matchingLogins, matchingFuzzyLogins);
                    return false;
                }
            }

            if(!loginUriString.Contains("://") && loginUriString.Contains("."))
            {
                loginUriString = "http://" + loginUriString;
            }
            string loginDomainName = null;
            if(Uri.TryCreate(loginUriString, UriKind.Absolute, out Uri loginUri)
                && DomainName.TryParseBaseDomain(loginUri.Host, out loginDomainName))
            {
                loginDomainName = loginDomainName.ToLowerInvariant();

                if(Array.IndexOf(matchingDomainsArray, loginDomainName) >= 0)
                {
                    AddMatchingLogin(cipher, matchingLogins, matchingFuzzyLogins);
                    return true;
                }
                else if(mobileApp && Array.IndexOf(matchingFuzzyDomainsArray, loginDomainName) >= 0)
                {
                    AddMatchingFuzzyLogin(cipher, matchingLogins, matchingFuzzyLogins);
                    return false;
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
                        var domainTerm = loginDomainName?.Split('.')[0];
                        addedFromSearchTerm =
                            (domainTerm != null && domainTerm.Length > 2 && term.Contains(domainTerm)) ||
                            (loginNameString != null && term.Contains(loginNameString));
                    }

                    if(addedFromSearchTerm)
                    {
                        AddMatchingFuzzyLogin(cipher, matchingLogins, matchingFuzzyLogins);
                        return false;
                    }
                }
            }

            return false;
        }

        private void AddMatchingLogin(Cipher cipher, List<Cipher> matchingLogins, List<Cipher> matchingFuzzyLogins)
        {
            if(matchingFuzzyLogins.Contains(cipher))
            {
                matchingFuzzyLogins.Remove(cipher);
            }

            matchingLogins.Add(cipher);
        }

        private void AddMatchingFuzzyLogin(Cipher cipher, List<Cipher> matchingLogins, List<Cipher> matchingFuzzyLogins)
        {
            if(!matchingFuzzyLogins.Contains(cipher) && !matchingLogins.Contains(cipher))
            {
                matchingFuzzyLogins.Add(cipher);
            }
        }
    }
}
