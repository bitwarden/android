//#define ENABLE_NEW_CIPHER_KEY_ENCRYPTION_ON_CREATION

using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net.Http;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Enums;
using Bit.Core.Exceptions;
using Bit.Core.Models.Data;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Request;
using Bit.Core.Models.Response;
using Bit.Core.Models.View;
using Bit.Core.Utilities;
using View = Bit.Core.Models.View.View;

namespace Bit.Core.Services
{
    public class CipherService : ICipherService
    {
        private readonly string[] _ignoredSearchTerms = new string[] { "com", "net", "org", "android",
            "io", "co", "uk", "au", "nz", "fr", "de", "tv", "info", "app", "apps", "eu", "me", "dev", "jp", "mobile" };
        private List<CipherView> _decryptedCipherCache;
        private readonly ICryptoService _cryptoService;
        private readonly IStateService _stateService;
        private readonly ISettingsService _settingsService;
        private readonly IApiService _apiService;
        private readonly IFileUploadService _fileUploadService;
        private readonly IStorageService _storageService;
        private readonly II18nService _i18nService;
        private readonly Func<ISearchService> _searchService;
        private readonly IConfigService _configService;
        private readonly ITotpService _totpService;
        private readonly IClipboardService _clipboardService;
        private readonly string _clearCipherCacheKey;
        private readonly string[] _allClearCipherCacheKeys;
        private Dictionary<string, HashSet<string>> _domainMatchBlacklist = new Dictionary<string, HashSet<string>>
        {
            ["google.com"] = new HashSet<string> { "script.google.com" }
        };
        private readonly HttpClient _httpClient = new HttpClient();
        private Task<List<CipherView>> _getAllDecryptedTask;

        public CipherService(
            ICryptoService cryptoService,
            IStateService stateService,
            ISettingsService settingsService,
            IApiService apiService,
            IFileUploadService fileUploadService,
            IStorageService storageService,
            II18nService i18nService,
            Func<ISearchService> searchService,
            IConfigService configService,
            ITotpService totpService,
            IClipboardService clipboardService,
            string clearCipherCacheKey,
            string[] allClearCipherCacheKeys)
        {
            _cryptoService = cryptoService;
            _stateService = stateService;
            _settingsService = settingsService;
            _apiService = apiService;
            _fileUploadService = fileUploadService;
            _storageService = storageService;
            _i18nService = i18nService;
            _searchService = searchService;
            _configService = configService;
            _totpService = totpService;
            _clipboardService = clipboardService;
            _clearCipherCacheKey = clearCipherCacheKey;
            _allClearCipherCacheKeys = allClearCipherCacheKeys;
        }

        private List<CipherView> DecryptedCipherCache
        {
            get => _decryptedCipherCache;
            set
            {
                if (value == null)
                {
                    _decryptedCipherCache?.Clear();
                }
                _decryptedCipherCache = value;
                if (_searchService != null)
                {
                    if (value == null)
                    {
                        _searchService().ClearIndex();
                    }
                    else
                    {
                        _searchService().IndexCiphersAsync();
                    }
                }
            }
        }

        public async Task ClearCacheAsync()
        {
            DecryptedCipherCache = null;
            if (_allClearCipherCacheKeys != null && _allClearCipherCacheKeys.Length > 0)
            {
                foreach (var key in _allClearCipherCacheKeys)
                {
                    await _storageService.SaveAsync(key, true);
                }
            }
        }

        public async Task<Cipher> EncryptAsync(CipherView model, SymmetricCryptoKey key = null,
            Cipher originalCipher = null)
        {
            // Adjust password history
            if (model.Id != null)
            {
                if (originalCipher == null)
                {
                    originalCipher = await GetAsync(model.Id);
                }
                if (originalCipher != null)
                {
                    var existingCipher = await originalCipher.DecryptAsync();
                    if (model.PasswordHistory == null)
                    {
                        model.PasswordHistory = new List<PasswordHistoryView>();
                    }
                    if (model.Type == CipherType.Login && existingCipher.Type == CipherType.Login)
                    {
                        if (!string.IsNullOrWhiteSpace(existingCipher.Login.Password) &&
                            existingCipher.Login.Password != model.Login.Password)
                        {
                            var now = DateTime.UtcNow;
                            var ph = new PasswordHistoryView
                            {
                                Password = existingCipher.Login.Password,
                                LastUsedDate = now
                            };
                            model.Login.PasswordRevisionDate = now;
                            model.PasswordHistory.Insert(0, ph);
                        }
                        else
                        {
                            model.Login.PasswordRevisionDate = existingCipher.Login.PasswordRevisionDate;
                        }
                    }
                    if (existingCipher.HasFields)
                    {
                        var existingHiddenFields = existingCipher.Fields.Where(f =>
                            f.Type == FieldType.Hidden && !string.IsNullOrWhiteSpace(f.Name) &&
                            !string.IsNullOrWhiteSpace(f.Value));
                        var hiddenFields = model.Fields?.Where(f =>
                            f.Type == FieldType.Hidden && !string.IsNullOrWhiteSpace(f.Name)) ??
                            new List<FieldView>();
                        foreach (var ef in existingHiddenFields)
                        {
                            var matchedField = hiddenFields.FirstOrDefault(f => f.Name == ef.Name);
                            if (matchedField == null || matchedField.Value != ef.Value)
                            {
                                var ph = new PasswordHistoryView
                                {
                                    Password = string.Format("{0}: {1}", ef.Name, ef.Value),
                                    LastUsedDate = DateTime.UtcNow
                                };
                                model.PasswordHistory.Insert(0, ph);
                            }
                        }
                    }
                }
                if (!model.PasswordHistory?.Any() ?? false)
                {
                    model.PasswordHistory = null;
                }
                else if (model.PasswordHistory != null && model.PasswordHistory.Count > 5)
                {
                    model.PasswordHistory = model.PasswordHistory.Take(5).ToList();
                }
            }

            var cipher = new Cipher
            {
                Id = model.Id,
                FolderId = model.FolderId,
                Favorite = model.Favorite,
                OrganizationId = model.OrganizationId,
                Type = model.Type,
                CollectionIds = model.CollectionIds,
                CreationDate = model.CreationDate,
                RevisionDate = model.RevisionDate,
                Reprompt = model.Reprompt
            };

            key = await UpdateCipherAndGetCipherKeyAsync(cipher, model, key);

            var tasks = new List<Task>
            {
                EncryptObjPropertyAsync(model, cipher, new HashSet<string>
                {
                   nameof(CipherView.Name),
                   nameof(CipherView.Notes)
                }, key),
                EncryptCipherDataAsync(cipher, model, key),
                EncryptFieldsAsync(model.Fields, key, cipher),
                EncryptPasswordHistoriesAsync(model.PasswordHistory, key, cipher),
                EncryptAttachmentsAsync(model.Attachments, key, cipher)
            };
            await Task.WhenAll(tasks);
            return cipher;
        }

        private async Task<SymmetricCryptoKey> UpdateCipherAndGetCipherKeyAsync(Cipher cipher, CipherView cipherView, SymmetricCryptoKey key = null, bool shouldCreateNewCipherKeyIfNeeded = true)
        {
            if (key == null && cipher.OrganizationId != null)
            {
                key = await _cryptoService.GetOrgKeyAsync(cipher.OrganizationId);
                if (key == null)
                {
                    throw new Exception("Cannot encrypt cipher for organization. No key.");
                }
            }

            if (!await ShouldUseCipherKeyEncryptionAsync())
            {
                return key;
            }

            if (cipherView.Key != null)
            {
                cipher.Key = await _cryptoService.EncryptAsync(cipherView.Key.Key, key);
                return cipherView.Key;
            }

            if (!shouldCreateNewCipherKeyIfNeeded)
            {
                return key;
            }

#if ENABLE_NEW_CIPHER_KEY_ENCRYPTION_ON_CREATION
            // turned on, only on debug to check that the enc/decryption is working fine at the cipher level.
            // this will be allowed on production on a later release.
            var cfs = ServiceContainer.Resolve<ICryptoFunctionService>();
            var newKey = new SymmetricCryptoKey(await cfs.RandomBytesAsync(Core.Constants.CipherKeyRandomBytesLength));
            cipher.Key = await _cryptoService.EncryptAsync(newKey.Key, key);

            return newKey;
#else
            return key;
#endif
        }

        private async Task<bool> ShouldUseCipherKeyEncryptionAsync()
        {
            var config = await _configService.GetAsync();

            return config != null
                   &&
                   VersionHelpers.IsServerVersionGreaterThanOrEqualTo(config.Version, Constants.CipherKeyEncryptionMinServerVersion);
        }

        public async Task<Cipher> GetAsync(string id)
        {
            var localData = await _stateService.GetCiphersLocalDataAsync();
            var ciphers = await _stateService.GetEncryptedCiphersAsync();
            if (!ciphers?.ContainsKey(id) ?? true)
            {
                return null;
            }
            return new Cipher(ciphers[id], false,
                localData?.ContainsKey(id) ?? false ? localData[id] : null);
        }

        public async Task<List<Cipher>> GetAllAsync()
        {
            var localData = await _stateService.GetCiphersLocalDataAsync();
            var ciphers = await _stateService.GetEncryptedCiphersAsync();
            var response = ciphers?.Select(c => new Cipher(c.Value, false,
                localData?.ContainsKey(c.Key) ?? false ? localData[c.Key] : null));
            return response?.ToList() ?? new List<Cipher>();
        }

        public async Task<List<CipherView>> GetAllDecryptedAsync(Func<Cipher, bool> filter = null)
        {
            if (_clearCipherCacheKey != null)
            {
                var clearCache = await _storageService.GetAsync<bool?>(_clearCipherCacheKey);
                if (clearCache.GetValueOrDefault())
                {
                    DecryptedCipherCache = null;
                    await _storageService.RemoveAsync(_clearCipherCacheKey);
                }
            }
            if (DecryptedCipherCache != null && filter is null)
            {
                return DecryptedCipherCache;
            }
            if (_getAllDecryptedTask != null && !_getAllDecryptedTask.IsCompleted && !_getAllDecryptedTask.IsFaulted)
            {
                return await _getAllDecryptedTask;
            }
            async Task<List<CipherView>> doTask()
            {
                try
                {
                    if (!await _cryptoService.HasUserKeyAsync())
                    {
                        throw new UserKeyNullException();
                    }
                    var decCiphers = new List<CipherView>();
                    async Task decryptAndAddCipherAsync(Cipher cipher)
                    {
                        var c = await cipher.DecryptAsync();
                        decCiphers.Add(c);
                    }
                    var tasks = new List<Task>();
                    IEnumerable<Cipher> ciphers = await GetAllAsync();
                    if (filter != null)
                    {
                        ciphers = ciphers.Where(filter);
                    }

                    foreach (var cipher in ciphers)
                    {
                        tasks.Add(decryptAndAddCipherAsync(cipher));
                    }
                    await Task.WhenAll(tasks);
                    decCiphers = decCiphers.OrderBy(c => c, new CipherLocaleComparer(_i18nService)).ToList();

                    if (filter != null)
                    {
                        return decCiphers;
                    }

                    DecryptedCipherCache = decCiphers;
                    return DecryptedCipherCache;
                }
                finally { }
            }
            _getAllDecryptedTask = doTask();
            return await _getAllDecryptedTask;
        }

        public async Task<List<CipherView>> GetAllDecryptedForGroupingAsync(string groupingId, bool folder = true)
        {
            var ciphers = await GetAllDecryptedAsync();
            return ciphers.Where(cipher =>
            {
                if (cipher.IsDeleted)
                {
                    return false;
                }
                if (folder && cipher.FolderId == groupingId)
                {
                    return true;
                }
                if (!folder && cipher.CollectionIds != null && cipher.CollectionIds.Contains(groupingId))
                {
                    return true;
                }
                return false;
            }).ToList();
        }

        public async Task<List<CipherView>> GetAllDecryptedForUrlAsync(string url)
        {
            var all = await GetAllDecryptedByUrlAsync(url);
            return all.Item1;
        }

        public async Task<Tuple<List<CipherView>, List<CipherView>, List<CipherView>>> GetAllDecryptedByUrlAsync(
            string url, List<CipherType> includeOtherTypes = null)
        {
            if (string.IsNullOrWhiteSpace(url) && includeOtherTypes == null)
            {
                return new Tuple<List<CipherView>, List<CipherView>, List<CipherView>>(
                    new List<CipherView>(), new List<CipherView>(), new List<CipherView>());
            }

            var domain = CoreHelpers.GetDomain(url);
            var mobileApp = UrlIsMobileApp(url);

            var mobileAppInfo = InfoFromMobileAppUrl(url);
            var mobileAppWebUriString = mobileAppInfo?.Item1;
            var mobileAppSearchTerms = mobileAppInfo?.Item2;

            var matchingDomainsTask = GetMatchingDomainsAsync(url, domain, mobileApp, mobileAppWebUriString);
            var ciphersTask = GetAllDecryptedAsync();
            await Task.WhenAll(new List<Task>
            {
                matchingDomainsTask,
                ciphersTask
            });

            var matchingDomains = await matchingDomainsTask;
            var matchingDomainsSet = matchingDomains.Item1;
            var matchingFuzzyDomainsSet = matchingDomains.Item2;

            var matchingLogins = new List<CipherView>();
            var matchingFuzzyLogins = new List<CipherView>();
            var others = new List<CipherView>();
            var ciphers = await ciphersTask;

            var defaultMatch = (UriMatchType?)(await _stateService.GetDefaultUriMatchAsync());
            if (defaultMatch == null)
            {
                defaultMatch = UriMatchType.Domain;
            }

            foreach (var cipher in ciphers)
            {
                if (cipher.IsDeleted)
                {
                    continue;
                }

                if (cipher.Type != CipherType.Login && (includeOtherTypes?.Any(t => t == cipher.Type) ?? false))
                {
                    others.Add(cipher);
                    continue;
                }

                if (cipher.Type != CipherType.Login || cipher.Login?.Uris == null || !cipher.Login.Uris.Any())
                {
                    continue;
                }

                foreach (var u in cipher.Login.Uris)
                {
                    if (string.IsNullOrWhiteSpace(u.Uri))
                    {
                        continue;
                    }
                    var match = false;
                    var toMatch = defaultMatch;
                    if (u.Match != null)
                    {
                        toMatch = u.Match;
                    }
                    switch (toMatch)
                    {
                        case null:
                        case UriMatchType.Domain:
                            match = CheckDefaultUriMatch(cipher, u, matchingLogins, matchingFuzzyLogins,
                                matchingDomainsSet, matchingFuzzyDomainsSet, mobileApp, mobileAppSearchTerms);
                            if (match && u.Domain != null)
                            {
                                if (_domainMatchBlacklist.ContainsKey(u.Domain))
                                {
                                    var domainUrlHost = CoreHelpers.GetHost(url);
                                    if (_domainMatchBlacklist[u.Domain].Contains(domainUrlHost))
                                    {
                                        match = false;
                                    }
                                }
                            }
                            break;
                        case UriMatchType.Host:
                            var urlHost = CoreHelpers.GetHost(url);
                            match = urlHost != null && urlHost == CoreHelpers.GetHost(u.Uri);
                            if (match)
                            {
                                AddMatchingLogin(cipher, matchingLogins, matchingFuzzyLogins);
                            }
                            break;
                        case UriMatchType.Exact:
                            match = url == u.Uri;
                            if (match)
                            {
                                AddMatchingLogin(cipher, matchingLogins, matchingFuzzyLogins);
                            }
                            break;
                        case UriMatchType.StartsWith:
                            match = url.StartsWith(u.Uri);
                            if (match)
                            {
                                AddMatchingLogin(cipher, matchingLogins, matchingFuzzyLogins);
                            }
                            break;
                        case UriMatchType.RegularExpression:
                            try
                            {
                                var regex = new Regex(u.Uri, RegexOptions.IgnoreCase, TimeSpan.FromSeconds(1));
                                match = regex.IsMatch(url);
                                if (match)
                                {
                                    AddMatchingLogin(cipher, matchingLogins, matchingFuzzyLogins);
                                }
                            }
                            catch (ArgumentException) { }
                            break;
                        case UriMatchType.Never:
                        default:
                            break;
                    }
                    if (match)
                    {
                        break;
                    }
                }
            }
            return new Tuple<List<CipherView>, List<CipherView>, List<CipherView>>(
                matchingLogins, matchingFuzzyLogins, others);
        }

        public async Task<CipherView> GetLastUsedForUrlAsync(string url)
        {
            var ciphers = await GetAllDecryptedForUrlAsync(url);
            return ciphers.OrderBy(c => c, new CipherLastUsedComparer()).FirstOrDefault();
        }

        public async Task UpdateLastUsedDateAsync(string id)
        {
            var ciphersLocalData = await _stateService.GetCiphersLocalDataAsync();
            if (ciphersLocalData == null)
            {
                ciphersLocalData = new Dictionary<string, Dictionary<string, object>>();
            }
            if (!ciphersLocalData.ContainsKey(id))
            {
                ciphersLocalData.Add(id, new Dictionary<string, object>());
            }
            if (ciphersLocalData[id].ContainsKey("lastUsedDate"))
            {
                ciphersLocalData[id]["lastUsedDate"] = DateTime.UtcNow;
            }
            else
            {
                ciphersLocalData[id].Add("lastUsedDate", DateTime.UtcNow);
            }

            await _stateService.SetCiphersLocalDataAsync(ciphersLocalData);
            // Update cache
            if (DecryptedCipherCache == null)
            {
                return;
            }
            var cached = DecryptedCipherCache.FirstOrDefault(c => c.Id == id);
            if (cached != null)
            {
                cached.LocalData = ciphersLocalData[id];
            }
        }

        public async Task SaveWithServerAsync(Cipher cipher)
        {
            CipherResponse response;
            if (cipher.Id == null)
            {
                if (cipher.CollectionIds != null)
                {
                    var request = new CipherCreateRequest(cipher);
                    response = await _apiService.PostCipherCreateAsync(request);
                }
                else
                {
                    var request = new CipherRequest(cipher);
                    response = await _apiService.PostCipherAsync(request);
                }
                cipher.Id = response.Id;
            }
            else
            {
                var request = new CipherRequest(cipher);
                response = await _apiService.PutCipherAsync(cipher.Id, request);
            }

            var userId = await _stateService.GetActiveUserIdAsync();
            var data = new CipherData(response, userId, cipher.CollectionIds);
            await UpsertAsync(data);
        }

        public async Task ShareWithServerAsync(CipherView cipher, string organizationId, HashSet<string> collectionIds)
        {
            var attachmentTasks = new List<Task>();
            if (cipher.Attachments != null)
            {
                foreach (var attachment in cipher.Attachments)
                {
                    if (attachment.Key == null)
                    {
                        attachmentTasks.Add(ShareAttachmentWithServerAsync(attachment, cipher.Id, organizationId));
                    }
                }
            }
            await Task.WhenAll(attachmentTasks);
            cipher.OrganizationId = organizationId;
            cipher.CollectionIds = collectionIds;
            var encCipher = await EncryptAsync(cipher);
            var request = new CipherShareRequest(encCipher);
            var response = await _apiService.PutShareCipherAsync(cipher.Id, request);
            var userId = await _stateService.GetActiveUserIdAsync();
            var data = new CipherData(response, userId, collectionIds);
            await UpsertAsync(data);
        }

        public async Task<Cipher> SaveAttachmentRawWithServerAsync(Cipher cipher, CipherView cipherView, string filename, byte[] data)
        {
            var (attachmentKey, protectedAttachmentKey, encKey) = await MakeAttachmentKeyAsync(cipher.OrganizationId, cipher, cipherView);

            var encFileName = await _cryptoService.EncryptAsync(filename, encKey);
            var encFileData = await _cryptoService.EncryptToBytesAsync(data, attachmentKey);

            CipherResponse response;
            try
            {
                var request = new AttachmentRequest
                {
                    Key = protectedAttachmentKey.EncryptedString,
                    FileName = encFileName.EncryptedString,
                    FileSize = encFileData.Buffer.Length,
                };

                var uploadDataResponse = await _apiService.PostCipherAttachmentAsync(cipher.Id, request);
                response = uploadDataResponse.CipherResponse;

                await _fileUploadService.UploadCipherAttachmentFileAsync(uploadDataResponse, encFileName, encFileData);
            }
            catch (ApiException e) when (e.Error.StatusCode == System.Net.HttpStatusCode.NotFound || e.Error.StatusCode == System.Net.HttpStatusCode.MethodNotAllowed)
            {
                response = await LegacyServerAttachmentFileUploadAsync(cipher.Id, encFileName, encFileData, protectedAttachmentKey);
            }

            var userId = await _stateService.GetActiveUserIdAsync();
            var cData = new CipherData(response, userId, cipher.CollectionIds);
            await UpsertAsync(cData);
            return new Cipher(cData);
        }

        [Obsolete("Mar 25 2021: This method has been deprecated in favor of direct uploads. This method still exists for backward compatibility with old server versions.")]
        private async Task<CipherResponse> LegacyServerAttachmentFileUploadAsync(string cipherId,
            EncString encFileName, EncByteArray encFileData, EncString key)
        {
            var boundary = string.Concat("--BWMobileFormBoundary", DateTime.UtcNow.Ticks);
            var fd = new MultipartFormDataContent(boundary);
            fd.Add(new StringContent(key.EncryptedString), "key");
            fd.Add(new StreamContent(new MemoryStream(encFileData.Buffer)), "data", encFileName.EncryptedString);
            return await _apiService.PostCipherAttachmentLegacyAsync(cipherId, fd);
        }

        public async Task SaveCollectionsWithServerAsync(Cipher cipher)
        {
            var request = new CipherCollectionsRequest(cipher.CollectionIds?.ToList());
            await _apiService.PutCipherCollectionsAsync(cipher.Id, request);
            var userId = await _stateService.GetActiveUserIdAsync();
            var data = cipher.ToCipherData(userId);
            await UpsertAsync(data);
        }

        public async Task UpsertAsync(CipherData cipher)
        {
            var ciphers = await _stateService.GetEncryptedCiphersAsync();
            if (ciphers == null)
            {
                ciphers = new Dictionary<string, CipherData>();
            }
            if (!ciphers.ContainsKey(cipher.Id))
            {
                ciphers.Add(cipher.Id, null);
            }
            ciphers[cipher.Id] = cipher;
            await _stateService.SetEncryptedCiphersAsync(ciphers);
            await ClearCacheAsync();
        }

        public async Task UpsertAsync(List<CipherData> cipher)
        {
            var ciphers = await _stateService.GetEncryptedCiphersAsync();
            if (ciphers == null)
            {
                ciphers = new Dictionary<string, CipherData>();
            }
            foreach (var c in cipher)
            {
                if (!ciphers.ContainsKey(c.Id))
                {
                    ciphers.Add(c.Id, null);
                }
                ciphers[c.Id] = c;
            }
            await _stateService.SetEncryptedCiphersAsync(ciphers);
            await ClearCacheAsync();
        }

        public async Task ReplaceAsync(Dictionary<string, CipherData> ciphers)
        {
            await _stateService.SetEncryptedCiphersAsync(ciphers);
            await ClearCacheAsync();
        }

        public async Task ClearAsync(string userId)
        {
            await _stateService.SetEncryptedCiphersAsync(null, userId);
            await ClearCacheAsync();
        }

        public async Task DeleteAsync(string id)
        {
            var ciphers = await _stateService.GetEncryptedCiphersAsync();
            if (ciphers == null)
            {
                return;
            }
            if (!ciphers.ContainsKey(id))
            {
                return;
            }
            ciphers.Remove(id);
            await _stateService.SetEncryptedCiphersAsync(ciphers);
            await ClearCacheAsync();
        }

        public async Task DeleteAsync(List<string> ids)
        {
            var ciphers = await _stateService.GetEncryptedCiphersAsync();
            if (ciphers == null)
            {
                return;
            }
            foreach (var id in ids)
            {
                if (!ciphers.ContainsKey(id))
                {
                    return;
                }
                ciphers.Remove(id);
            }
            await _stateService.SetEncryptedCiphersAsync(ciphers);
            await ClearCacheAsync();
        }

        public async Task DeleteWithServerAsync(string id)
        {
            await _apiService.DeleteCipherAsync(id);
            await DeleteAsync(id);
        }

        public async Task DeleteAttachmentAsync(string id, string attachmentId)
        {
            var ciphers = await _stateService.GetEncryptedCiphersAsync();
            if (ciphers == null || !ciphers.ContainsKey(id) || ciphers[id].Attachments == null)
            {
                return;
            }
            var attachment = ciphers[id].Attachments.FirstOrDefault(a => a.Id == attachmentId);
            if (attachment != null)
            {
                ciphers[id].Attachments.Remove(attachment);
            }
            await _stateService.SetEncryptedCiphersAsync(ciphers);
            await ClearCacheAsync();
        }

        public async Task DeleteAttachmentWithServerAsync(string id, string attachmentId)
        {
            try
            {
                await _apiService.DeleteCipherAttachmentAsync(id, attachmentId);
                await DeleteAttachmentAsync(id, attachmentId);
            }
            catch (ApiException e)
            {
                await DeleteAttachmentAsync(id, attachmentId);
                throw;
            }
        }

        public async Task<byte[]> DownloadAndDecryptAttachmentAsync(string cipherId, AttachmentView attachment, string organizationId)
        {
            string url;
            try
            {
                var attachmentDownloadResponse = await _apiService.GetAttachmentData(cipherId, attachment.Id);
                url = attachmentDownloadResponse.Url;
            }
            // TODO: Delete this catch when all Servers are updated to respond to the above method
            catch (ApiException e) when (e.Error.StatusCode == System.Net.HttpStatusCode.NotFound)
            {
                url = attachment.Url;
            }

            try
            {
                var response = await _httpClient.GetAsync(new Uri(url));
                if (!response.IsSuccessStatusCode)
                {
                    return null;
                }
                var data = await response.Content.ReadAsByteArrayAsync();
                if (data == null)
                {
                    return null;
                }
                var key = attachment.Key ?? await _cryptoService.GetOrgKeyAsync(organizationId);
                return await _cryptoService.DecryptFromBytesAsync(data, key);
            }
            catch { }
            return null;
        }

        public async Task SoftDeleteWithServerAsync(string id)
        {
            var ciphers = await _stateService.GetEncryptedCiphersAsync();
            if (ciphers == null)
            {
                return;
            }
            if (!ciphers.ContainsKey(id))
            {
                return;
            }

            await _apiService.PutDeleteCipherAsync(id);
            ciphers[id].DeletedDate = DateTime.UtcNow;
            await _stateService.SetEncryptedCiphersAsync(ciphers);
            await ClearCacheAsync();
        }

        public async Task RestoreWithServerAsync(string id)
        {
            var ciphers = await _stateService.GetEncryptedCiphersAsync();
            if (ciphers == null)
            {
                return;
            }
            if (!ciphers.ContainsKey(id))
            {
                return;
            }
            var response = await _apiService.PutRestoreCipherAsync(id);
            ciphers[id].DeletedDate = null;
            ciphers[id].RevisionDate = response.RevisionDate;
            await _stateService.SetEncryptedCiphersAsync(ciphers);
            await ClearCacheAsync();
        }

        public async Task<bool> VerifyOrganizationHasUnassignedItemsAsync()
        {
            var organizations = await _stateService.GetOrganizationsAsync();
            if (organizations?.Any() != true)
            {
                return false;
            }

            try
            {
                return await _apiService.HasUnassignedCiphersAsync();
            }
            catch (ApiException ex) when (ex.Error?.StatusCode == System.Net.HttpStatusCode.BadRequest)
            {
                return false;
            }
        }

        // Helpers

        private async Task<Tuple<SymmetricCryptoKey, EncString, SymmetricCryptoKey>> MakeAttachmentKeyAsync(string organizationId, Cipher cipher = null, CipherView cipherView = null)
        {
            var orgKey = await _cryptoService.GetOrgKeyAsync(organizationId);

            SymmetricCryptoKey encryptionKey = orgKey;
            if (cipher != null && cipherView != null)
            {
                encryptionKey = await UpdateCipherAndGetCipherKeyAsync(cipher, cipherView, orgKey, false);
            }

            encryptionKey ??= await _cryptoService.GetUserKeyWithLegacySupportAsync();

            var (attachmentKey, protectedAttachmentKey) = await _cryptoService.MakeDataEncKeyAsync(encryptionKey);
            return new Tuple<SymmetricCryptoKey, EncString, SymmetricCryptoKey>(attachmentKey, protectedAttachmentKey, encryptionKey);
        }

        private async Task ShareAttachmentWithServerAsync(AttachmentView attachmentView, string cipherId,
            string organizationId)
        {
            var attachmentResponse = await _httpClient.GetAsync(attachmentView.Url);
            if (!attachmentResponse.IsSuccessStatusCode)
            {
                throw new Exception("Failed to download attachment: " + attachmentResponse.StatusCode);
            }

            var bytes = await attachmentResponse.Content.ReadAsByteArrayAsync();
            var decBytes = await _cryptoService.DecryptFromBytesAsync(bytes, null);

            var (attachmentKey, protectedAttachmentKey, encKey) = await MakeAttachmentKeyAsync(organizationId);

            var encFileName = await _cryptoService.EncryptAsync(attachmentView.FileName, encKey);
            var encFileData = await _cryptoService.EncryptToBytesAsync(decBytes, attachmentKey);

            var boundary = string.Concat("--BWMobileFormBoundary", DateTime.UtcNow.Ticks);
            var fd = new MultipartFormDataContent(boundary);
            fd.Add(new StringContent(protectedAttachmentKey.EncryptedString), "key");
            fd.Add(new StreamContent(new MemoryStream(encFileData.Buffer)), "data", encFileName.EncryptedString);
            await _apiService.PostShareCipherAttachmentAsync(cipherId, attachmentView.Id, fd, organizationId);
        }

        private bool CheckDefaultUriMatch(CipherView cipher, LoginUriView loginUri,
            List<CipherView> matchingLogins, List<CipherView> matchingFuzzyLogins,
            HashSet<string> matchingDomainsSet, HashSet<string> matchingFuzzyDomainsSet,
            bool mobileApp, string[] mobileAppSearchTerms)
        {
            var loginUriString = loginUri.Uri;
            var loginUriDomain = loginUri.Domain;

            if (matchingDomainsSet.Contains(loginUriString))
            {
                AddMatchingLogin(cipher, matchingLogins, matchingFuzzyLogins);
                return true;
            }
            else if (mobileApp && matchingFuzzyDomainsSet.Contains(loginUriString))
            {
                AddMatchingFuzzyLogin(cipher, matchingLogins, matchingFuzzyLogins);
                return false;
            }
            else if (!mobileApp)
            {
                var info = InfoFromMobileAppUrl(loginUriString);
                if (info?.Item1 != null && matchingDomainsSet.Contains(info.Item1))
                {
                    AddMatchingFuzzyLogin(cipher, matchingLogins, matchingFuzzyLogins);
                    return false;
                }
            }

            if (!loginUri.Uri.Contains("://") && loginUriString.Contains("."))
            {
                loginUriString = "http://" + loginUriString;
            }

            if (loginUriDomain != null)
            {
                loginUriDomain = loginUriDomain.ToLowerInvariant();
                if (matchingDomainsSet.Contains(loginUriDomain))
                {
                    AddMatchingLogin(cipher, matchingLogins, matchingFuzzyLogins);
                    return true;
                }
                else if (mobileApp && matchingFuzzyDomainsSet.Contains(loginUriDomain))
                {
                    AddMatchingFuzzyLogin(cipher, matchingLogins, matchingFuzzyLogins);
                    return false;
                }
            }

            if (mobileApp && (mobileAppSearchTerms?.Any() ?? false))
            {
                var addedFromSearchTerm = false;
                var loginName = cipher.Name?.ToLowerInvariant();
                foreach (var term in mobileAppSearchTerms)
                {
                    addedFromSearchTerm = (loginUriDomain != null && loginUriDomain.Contains(term)) ||
                        (loginName != null && loginName.Contains(term));
                    if (!addedFromSearchTerm)
                    {
                        var domainTerm = loginUriDomain?.Split('.')[0];
                        addedFromSearchTerm =
                            (domainTerm != null && domainTerm.Length > 2 && term.Contains(domainTerm)) ||
                            (loginName != null && term.Contains(loginName));
                    }
                    if (addedFromSearchTerm)
                    {
                        AddMatchingFuzzyLogin(cipher, matchingLogins, matchingFuzzyLogins);
                        return false;
                    }
                }
            }

            return false;
        }

        private void AddMatchingLogin(CipherView cipher, List<CipherView> matchingLogins,
            List<CipherView> matchingFuzzyLogins)
        {
            if (matchingFuzzyLogins.Contains(cipher))
            {
                matchingFuzzyLogins.Remove(cipher);
            }
            matchingLogins.Add(cipher);
        }

        private void AddMatchingFuzzyLogin(CipherView cipher, List<CipherView> matchingLogins,
            List<CipherView> matchingFuzzyLogins)
        {
            if (!matchingFuzzyLogins.Contains(cipher) && !matchingLogins.Contains(cipher))
            {
                matchingFuzzyLogins.Add(cipher);
            }
        }

        private async Task<Tuple<HashSet<string>, HashSet<string>>> GetMatchingDomainsAsync(string url,
            string domain, bool mobileApp, string mobileAppWebUriString)
        {
            var matchingDomains = new HashSet<string>();
            var matchingFuzzyDomains = new HashSet<string>();
            var eqDomains = await _settingsService.GetEquivalentDomainsAsync();
            foreach (var eqDomain in eqDomains)
            {
                var eqDomainSet = new HashSet<string>(eqDomain);
                if (mobileApp)
                {
                    if (eqDomainSet.Contains(url))
                    {
                        foreach (var d in eqDomain)
                        {
                            matchingDomains.Add(d);
                        }
                    }
                    else if (mobileAppWebUriString != null && eqDomainSet.Contains(mobileAppWebUriString))
                    {
                        foreach (var d in eqDomain)
                        {
                            matchingFuzzyDomains.Add(d);
                        }
                    }
                }
                else if (eqDomainSet.Contains(domain))
                {
                    foreach (var d in eqDomain)
                    {
                        matchingDomains.Add(d);
                    }
                }
            }
            if (!matchingDomains.Any())
            {
                matchingDomains.Add(mobileApp ? url : domain);
            }
            if (mobileApp && mobileAppWebUriString != null &&
                !matchingFuzzyDomains.Any() && !matchingDomains.Contains(mobileAppWebUriString))
            {
                matchingFuzzyDomains.Add(mobileAppWebUriString);
            }
            return new Tuple<HashSet<string>, HashSet<string>>(matchingDomains, matchingFuzzyDomains);
        }

        private Tuple<string, string[]> InfoFromMobileAppUrl(string mobileAppUrlString)
        {
            if (UrlIsAndroidApp(mobileAppUrlString))
            {
                return InfoFromAndroidAppUri(mobileAppUrlString);
            }
            else if (UrlIsiOSApp(mobileAppUrlString))
            {
                return InfoFromiOSAppUrl(mobileAppUrlString);
            }
            return null;
        }

        private Tuple<string, string[]> InfoFromAndroidAppUri(string androidAppUrlString)
        {
            if (!UrlIsAndroidApp(androidAppUrlString))
            {
                return null;
            }
            var androidUrlParts = androidAppUrlString.Replace(Constants.AndroidAppProtocol, string.Empty).Split('.');
            if (androidUrlParts.Length >= 2)
            {
                var webUri = string.Join(".", androidUrlParts[1], androidUrlParts[0]);
                var searchTerms = androidUrlParts.Where(p => !_ignoredSearchTerms.Contains(p))
                    .Select(p => p.ToLowerInvariant()).ToArray();
                return new Tuple<string, string[]>(webUri, searchTerms);
            }
            return null;
        }

        private Tuple<string, string[]> InfoFromiOSAppUrl(string iosAppUrlString)
        {
            if (!UrlIsiOSApp(iosAppUrlString))
            {
                return null;
            }
            var webUri = iosAppUrlString.Replace(Constants.iOSAppProtocol, string.Empty);
            return new Tuple<string, string[]>(webUri, null);
        }

        private bool UrlIsMobileApp(string url)
        {
            return UrlIsAndroidApp(url) || UrlIsiOSApp(url);
        }

        private bool UrlIsAndroidApp(string url)
        {
            return url.StartsWith(Constants.AndroidAppProtocol);
        }

        private bool UrlIsiOSApp(string url)
        {
            return url.StartsWith(Constants.iOSAppProtocol);
        }

        private Task EncryptObjPropertyAsync<V, D>(V model, D obj, HashSet<string> map, SymmetricCryptoKey key)
            where V : View
            where D : Domain
        {
            var modelType = model.GetType();
            var objType = obj.GetType();

            async Task makeAndSetCs(string propName)
            {
                var modelPropInfo = modelType.GetProperty(propName);
                var modelProp = modelPropInfo.GetValue(model) as string;
                EncString val = null;
                if (!string.IsNullOrWhiteSpace(modelProp))
                {
                    val = await _cryptoService.EncryptAsync(modelProp, key);
                }
                var objPropInfo = objType.GetProperty(propName);
                objPropInfo.SetValue(obj, val, null);
            };

            var tasks = new List<Task>();
            foreach (var prop in map)
            {
                tasks.Add(makeAndSetCs(prop));
            }
            return Task.WhenAll(tasks);
        }

        private async Task EncryptAttachmentsAsync(List<AttachmentView> attachmentsModel, SymmetricCryptoKey key,
            Cipher cipher)
        {
            if (!attachmentsModel?.Any() ?? true)
            {
                cipher.Attachments = null;
                return;
            }
            var tasks = new List<Task>();
            var encAttachments = new List<Attachment>();
            async Task encryptAndAddAttachmentAsync(AttachmentView model, Attachment attachment)
            {
                await EncryptObjPropertyAsync(model, attachment, new HashSet<string>
                {
                    nameof(AttachmentView.FileName)
                }, key);
                if (model.Key != null)
                {
                    attachment.Key = await _cryptoService.EncryptAsync(model.Key.Key, key);
                }
                encAttachments.Add(attachment);
            }
            foreach (var model in attachmentsModel)
            {
                tasks.Add(encryptAndAddAttachmentAsync(model, new Attachment
                {
                    Id = model.Id,
                    Size = model.Size,
                    SizeName = model.SizeName,
                    Url = model.Url
                }));
            }
            await Task.WhenAll(tasks);
            cipher.Attachments = encAttachments;
        }

        private async Task EncryptCipherDataAsync(Cipher cipher, CipherView model, SymmetricCryptoKey key)
        {
            switch (cipher.Type)
            {
                case CipherType.Login:
                    cipher.Login = new Login
                    {
                        PasswordRevisionDate = model.Login.PasswordRevisionDate
                    };
                    await EncryptObjPropertyAsync(model.Login, cipher.Login, new HashSet<string>
                    {
                        "Username",
                        "Password",
                        "Totp"
                    }, key);
                    if (model.Login.Uris != null)
                    {
                        cipher.Login.Uris = new List<LoginUri>();
                        foreach (var uri in model.Login.Uris.Where(u => u.Uri != null))
                        {
                            var loginUri = new LoginUri
                            {
                                Match = uri.Match
                            };
                            await EncryptObjPropertyAsync(uri, loginUri, new HashSet<string> { "Uri" }, key);
                            var uriHash = await _cryptoService.HashAsync(uri.Uri, CryptoHashAlgorithm.Sha256);
                            loginUri.UriChecksum = await _cryptoService.EncryptAsync(uriHash, key);
                            cipher.Login.Uris.Add(loginUri);
                        }
                    }
                    if (model.Login.HasFido2Credentials)
                    {
                        cipher.Login.Fido2Credentials = new List<Fido2Credential>();
                        foreach (var fido2Credential in model.Login.Fido2Credentials)
                        {
                            var fido2CredentialDomain = new Fido2Credential
                            {
                                CreationDate = fido2Credential.CreationDate
                            };
                            await EncryptObjPropertyAsync(fido2Credential, fido2CredentialDomain, Fido2Credential.EncryptablePropertiesToMap, key);
                            cipher.Login.Fido2Credentials.Add(fido2CredentialDomain);
                        }
                    }
                    break;
                case CipherType.SecureNote:
                    cipher.SecureNote = new SecureNote
                    {
                        Type = model.SecureNote.Type
                    };
                    break;
                case CipherType.Card:
                    cipher.Card = new Card();
                    await EncryptObjPropertyAsync(model.Card, cipher.Card, new HashSet<string>
                    {
                        "CardholderName",
                        "Brand",
                        "Number",
                        "ExpMonth",
                        "ExpYear",
                        "Code"
                    }, key);
                    break;
                case CipherType.Identity:
                    cipher.Identity = new Identity();
                    await EncryptObjPropertyAsync(model.Identity, cipher.Identity, new HashSet<string>
                    {
                        "Title",
                        "FirstName",
                        "MiddleName",
                        "LastName",
                        "Address1",
                        "Address2",
                        "Address3",
                        "City",
                        "State",
                        "PostalCode",
                        "Country",
                        "Company",
                        "Email",
                        "Phone",
                        "SSN",
                        "Username",
                        "PassportNumber",
                        "LicenseNumber"
                    }, key);
                    break;
                default:
                    throw new Exception("Unknown cipher type.");
            }
        }

        private async Task EncryptFieldsAsync(List<FieldView> fieldsModel, SymmetricCryptoKey key,
            Cipher cipher)
        {
            if (!fieldsModel?.Any() ?? true)
            {
                cipher.Fields = null;
                return;
            }
            var tasks = new List<Task>();
            var encFields = new List<Field>();
            async Task encryptAndAddFieldAsync(FieldView model, Field field)
            {
                await EncryptObjPropertyAsync(model, field, new HashSet<string>
                {
                    "Name",
                    "Value"
                }, key);
                encFields.Add(field);
            }
            foreach (var model in fieldsModel)
            {
                var field = new Field
                {
                    Type = model.Type,
                    LinkedId = model.LinkedId,
                };
                // normalize boolean type field values
                if (model.Type == FieldType.Boolean && model.Value != "true")
                {
                    model.Value = "false";
                }
                tasks.Add(encryptAndAddFieldAsync(model, field));
            }
            await Task.WhenAll(tasks);
            cipher.Fields = encFields;
        }

        private async Task EncryptPasswordHistoriesAsync(List<PasswordHistoryView> phModels,
            SymmetricCryptoKey key, Cipher cipher)
        {
            if (!phModels?.Any() ?? true)
            {
                cipher.PasswordHistory = null;
                return;
            }
            var tasks = new List<Task>();
            var encPhs = new List<PasswordHistory>();
            async Task encryptAndAddHistoryAsync(PasswordHistoryView model, PasswordHistory ph)
            {
                await EncryptObjPropertyAsync(model, ph, new HashSet<string>
                {
                    "Password"
                }, key);
                encPhs.Add(ph);
            }
            foreach (var model in phModels)
            {
                tasks.Add(encryptAndAddHistoryAsync(model, new PasswordHistory
                {
                    LastUsedDate = model.LastUsedDate
                }));
            }
            await Task.WhenAll(tasks);
            cipher.PasswordHistory = encPhs;
        }

        public async Task<string> CreateNewLoginForPasskeyAsync(Fido2ConfirmNewCredentialParams newPasskeyParams)
        {
            var newCipher = new CipherView
            {
                Name = newPasskeyParams.CredentialName,
                Type = CipherType.Login,
                Login = new LoginView
                {
                    Username = newPasskeyParams.UserName,
                    Uris = new List<LoginUriView>
                    {
                        new LoginUriView { Uri = newPasskeyParams.RpId }
                    }
                },
                Card = new CardView(),
                Identity = new IdentityView(),
                SecureNote = new SecureNoteView
                {
                    Type = SecureNoteType.Generic
                },
                Reprompt = CipherRepromptType.None
            };

            var encryptedCipher = await EncryptAsync(newCipher);
            await SaveWithServerAsync(encryptedCipher);

            return encryptedCipher.Id;
        }

        public async Task CopyTotpCodeIfNeededAsync(CipherView cipher)
        {
            if (string.IsNullOrWhiteSpace(cipher?.Login?.Totp)
                ||
                await _stateService.GetDisableAutoTotpCopyAsync() == true)
            {
                return;
            }

            if (cipher.OrganizationUseTotp || await _stateService.CanAccessPremiumAsync())
            {
                var totpCode = await _totpService.GetCodeAsync(cipher.Login.Totp);
                await _clipboardService.CopyTextAsync(totpCode);
            }
        }

        private class CipherLocaleComparer : IComparer<CipherView>
        {
            private readonly II18nService _i18nService;

            public CipherLocaleComparer(II18nService i18nService)
            {
                _i18nService = i18nService;
            }

            public int Compare(CipherView a, CipherView b)
            {
                var aName = a?.ComparableName;
                var bName = b?.ComparableName;
                if (aName == null && bName != null)
                {
                    return -1;
                }
                if (aName != null && bName == null)
                {
                    return 1;
                }
                if (aName == null && bName == null)
                {
                    return 0;
                }
                return _i18nService.StringComparer.Compare(aName, bName);
            }
        }

        private class CipherLastUsedComparer : IComparer<CipherView>
        {
            public int Compare(CipherView a, CipherView b)
            {
                var aLastUsed = a.LocalData != null && a.LocalData.ContainsKey("lastUsedDate") ?
                    a.LocalData["lastUsedDate"] as DateTime? : null;
                var bLastUsed = b.LocalData != null && b.LocalData.ContainsKey("lastUsedDate") ?
                    b.LocalData["lastUsedDate"] as DateTime? : null;

                var bothNotNull = aLastUsed != null && bLastUsed != null;
                if (bothNotNull && aLastUsed.Value < bLastUsed.Value)
                {
                    return 1;
                }
                if (aLastUsed != null && bLastUsed == null)
                {
                    return -1;
                }
                if (bothNotNull && aLastUsed.Value > bLastUsed.Value)
                {
                    return -1;
                }
                if (bLastUsed != null && aLastUsed == null)
                {
                    return 1;
                }
                return 0;
            }
        }

        private class CipherLastUsedThenNameComparer : IComparer<CipherView>
        {
            private CipherLastUsedComparer _cipherLastUsedComparer;
            private CipherLocaleComparer _cipherLocaleComparer;

            public CipherLastUsedThenNameComparer(II18nService i18nService)
            {
                _cipherLastUsedComparer = new CipherLastUsedComparer();
                _cipherLocaleComparer = new CipherLocaleComparer(i18nService);
            }

            public int Compare(CipherView a, CipherView b)
            {
                var result = _cipherLastUsedComparer.Compare(a, b);
                if (result != 0)
                {
                    return result;
                }
                return _cipherLocaleComparer.Compare(a, b);
            }
        }
    }
}
