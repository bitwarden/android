using Bit.Core.Abstractions;
using Bit.Core.Exceptions;
using Bit.Core.Models.Data;
using Bit.Core.Models.Response;
using Bit.Core.Utilities;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace Bit.Core.Services
{
    public class SyncService : ISyncService
    {
        private const string Keys_LastSyncFormat = "lastSync_{0}";

        private readonly IUserService _userService;
        private readonly IApiService _apiService;
        private readonly ISettingsService _settingsService;
        private readonly IFolderService _folderService;
        private readonly ICipherService _cipherService;
        private readonly ICryptoService _cryptoService;
        private readonly ICollectionService _collectionService;
        private readonly IStorageService _storageService;
        private readonly IMessagingService _messagingService;
        private readonly IPolicyService _policyService;
        private readonly ISendService _sendService;
        private readonly Func<bool, Task> _logoutCallbackAsync;

        public SyncService(
            IUserService userService,
            IApiService apiService,
            ISettingsService settingsService,
            IFolderService folderService,
            ICipherService cipherService,
            ICryptoService cryptoService,
            ICollectionService collectionService,
            IStorageService storageService,
            IMessagingService messagingService,
            IPolicyService policyService,
            ISendService sendService,
            Func<bool, Task> logoutCallbackAsync)
        {
            _userService = userService;
            _apiService = apiService;
            _settingsService = settingsService;
            _folderService = folderService;
            _cipherService = cipherService;
            _cryptoService = cryptoService;
            _collectionService = collectionService;
            _storageService = storageService;
            _messagingService = messagingService;
            _policyService = policyService;
            _sendService = sendService;
            _logoutCallbackAsync = logoutCallbackAsync;
        }

        public bool SyncInProgress { get; set; }

        public async Task<DateTime?> GetLastSyncAsync()
        {
            var userId = await _userService.GetUserIdAsync();
            if (userId == null)
            {
                return null;
            }
            return await _storageService.GetAsync<DateTime?>(string.Format(Keys_LastSyncFormat, userId));
        }

        public async Task SetLastSyncAsync(DateTime date)
        {
            var userId = await _userService.GetUserIdAsync();
            if (userId == null)
            {
                return;
            }
            await _storageService.SaveAsync(string.Format(Keys_LastSyncFormat, userId), date);
        }

        public async Task<bool> FullSyncAsync(bool forceSync, bool allowThrowOnError = false)
        {
            SyncStarted();
            var isAuthenticated = await _userService.IsAuthenticatedAsync();
            if (!isAuthenticated)
            {
                return SyncCompleted(false);
            }
            var now = DateTime.UtcNow;
            var needsSyncResult = await NeedsSyncingAsync(forceSync);
            var needsSync = needsSyncResult.Item1;
            var skipped = needsSyncResult.Item2;
            if (skipped)
            {
                return SyncCompleted(false);
            }
            if (!needsSync)
            {
                await SetLastSyncAsync(now);
                return SyncCompleted(false);
            }
            var userId = await _userService.GetUserIdAsync();
            try
            {
                var response = await _apiService.GetSyncAsync();
                await SyncProfileAsync(response.Profile);
                await SyncFoldersAsync(userId, response.Folders);
                await SyncCollectionsAsync(response.Collections);
                await SyncCiphersAsync(userId, response.Ciphers);
                await SyncSettingsAsync(userId, response.Domains);
                await SyncPoliciesAsync(response.Policies);
                await SyncSendsAsync(userId, response.Sends);
                await SetLastSyncAsync(now);
                return SyncCompleted(true);
            }
            catch
            {
                if (allowThrowOnError)
                {
                    throw;
                }
                else
                {
                    return SyncCompleted(false);
                }
            }
        }

        public async Task<bool> SyncUpsertFolderAsync(SyncFolderNotification notification, bool isEdit)
        {
            SyncStarted();
            if (await _userService.IsAuthenticatedAsync())
            {
                try
                {
                    var localFolder = await _folderService.GetAsync(notification.Id);
                    if ((!isEdit && localFolder == null) ||
                        (isEdit && localFolder != null && localFolder.RevisionDate < notification.RevisionDate))
                    {
                        var remoteFolder = await _apiService.GetFolderAsync(notification.Id);
                        if (remoteFolder != null)
                        {
                            var userId = await _userService.GetUserIdAsync();
                            await _folderService.UpsertAsync(new FolderData(remoteFolder, userId));
                            _messagingService.Send("syncedUpsertedFolder", new Dictionary<string, string>
                            {
                                ["folderId"] = notification.Id
                            });
                            return SyncCompleted(true);
                        }
                    }
                }
                catch { }
            }
            return SyncCompleted(false);
        }

        public async Task<bool> SyncDeleteFolderAsync(SyncFolderNotification notification)
        {
            SyncStarted();
            if (await _userService.IsAuthenticatedAsync())
            {
                await _folderService.DeleteAsync(notification.Id);
                _messagingService.Send("syncedDeletedFolder", new Dictionary<string, string>
                {
                    ["folderId"] = notification.Id
                });
                return SyncCompleted(true);
            }
            return SyncCompleted(false);
        }

        public async Task<bool> SyncUpsertCipherAsync(SyncCipherNotification notification, bool isEdit)
        {
            SyncStarted();
            if (await _userService.IsAuthenticatedAsync())
            {
                try
                {
                    var shouldUpdate = true;
                    var localCipher = await _cipherService.GetAsync(notification.Id);
                    if (localCipher != null && localCipher.RevisionDate >= notification.RevisionDate)
                    {
                        shouldUpdate = false;
                    }

                    var checkCollections = false;
                    if (shouldUpdate)
                    {
                        if (isEdit)
                        {
                            shouldUpdate = localCipher != null;
                            checkCollections = true;
                        }
                        else
                        {
                            if (notification.CollectionIds == null || notification.OrganizationId == null)
                            {
                                shouldUpdate = localCipher == null;
                            }
                            else
                            {
                                shouldUpdate = false;
                                checkCollections = true;
                            }
                        }
                    }

                    if (!shouldUpdate && checkCollections && notification.OrganizationId != null &&
                        notification.CollectionIds != null && notification.CollectionIds.Any())
                    {
                        var collections = await _collectionService.GetAllAsync();
                        if (collections != null)
                        {
                            foreach (var c in collections)
                            {
                                if (notification.CollectionIds.Contains(c.Id))
                                {
                                    shouldUpdate = true;
                                    break;
                                }
                            }
                        }
                    }

                    if (shouldUpdate)
                    {
                        var remoteCipher = await _apiService.GetCipherAsync(notification.Id);
                        if (remoteCipher != null)
                        {
                            var userId = await _userService.GetUserIdAsync();
                            await _cipherService.UpsertAsync(new CipherData(remoteCipher, userId));
                            _messagingService.Send("syncedUpsertedCipher", new Dictionary<string, string>
                            {
                                ["cipherId"] = notification.Id
                            });
                            return SyncCompleted(true);
                        }
                    }
                }
                catch (ApiException e)
                {
                    if (e.Error != null && e.Error.StatusCode == System.Net.HttpStatusCode.NotFound && isEdit)
                    {
                        await _cipherService.DeleteAsync(notification.Id);
                        _messagingService.Send("syncedDeletedCipher", new Dictionary<string, string>
                        {
                            ["cipherId"] = notification.Id
                        });
                        return SyncCompleted(true);
                    }
                }
            }
            return SyncCompleted(false);
        }

        public async Task<bool> SyncDeleteCipherAsync(SyncCipherNotification notification)
        {
            SyncStarted();
            if (await _userService.IsAuthenticatedAsync())
            {
                await _cipherService.DeleteAsync(notification.Id);
                _messagingService.Send("syncedDeletedCipher", new Dictionary<string, string>
                {
                    ["cipherId"] = notification.Id
                });
                return SyncCompleted(true);
            }
            return SyncCompleted(false);
        }

        // Helpers

        private void SyncStarted()
        {
            SyncInProgress = true;
            _messagingService.Send("syncStarted");
        }

        private bool SyncCompleted(bool successfully)
        {
            SyncInProgress = false;
            _messagingService.Send("syncCompleted", new Dictionary<string, object> { ["successfully"] = successfully });
            return successfully;
        }

        private async Task<Tuple<bool, bool>> NeedsSyncingAsync(bool forceSync)
        {
            if (forceSync)
            {
                return new Tuple<bool, bool>(true, false);
            }
            var lastSync = await GetLastSyncAsync();
            if (lastSync == null || lastSync == DateTime.MinValue)
            {
                return new Tuple<bool, bool>(true, false);
            }
            try
            {
                var response = await _apiService.GetAccountRevisionDateAsync();
                var d = CoreHelpers.Epoc.AddMilliseconds(response);
                if (d <= lastSync.Value)
                {
                    return new Tuple<bool, bool>(false, false);
                }
                return new Tuple<bool, bool>(true, false);
            }
            catch
            {
                return new Tuple<bool, bool>(false, true);
            }
        }

        private async Task SyncProfileAsync(ProfileResponse response)
        {
            var stamp = await _userService.GetSecurityStampAsync();
            if (stamp != null && stamp != response.SecurityStamp)
            {
                if (_logoutCallbackAsync != null)
                {
                    await _logoutCallbackAsync(true);
                }
                return;
            }
            await _cryptoService.SetEncKeyAsync(response.Key);
            await _cryptoService.SetEncPrivateKeyAsync(response.PrivateKey);
            await _cryptoService.SetOrgKeysAsync(response.Organizations);
            await _userService.SetSecurityStampAsync(response.SecurityStamp);
            var organizations = response.Organizations.ToDictionary(o => o.Id, o => new OrganizationData(o));
            await _userService.ReplaceOrganizationsAsync(organizations);
            await _userService.SetEmailVerifiedAsync(response.EmailVerified);
        }

        private async Task SyncFoldersAsync(string userId, List<FolderResponse> response)
        {
            var folders = response.ToDictionary(f => f.Id, f => new FolderData(f, userId));
            await _folderService.ReplaceAsync(folders);
        }

        private async Task SyncCollectionsAsync(List<CollectionDetailsResponse> response)
        {
            var collections = response.ToDictionary(c => c.Id, c => new CollectionData(c));
            await _collectionService.ReplaceAsync(collections);
        }

        private async Task SyncCiphersAsync(string userId, List<CipherResponse> response)
        {
            var ciphers = response.ToDictionary(c => c.Id, c => new CipherData(c, userId));
            await _cipherService.ReplaceAsync(ciphers);
        }

        private async Task SyncSettingsAsync(string userId, DomainsResponse response)
        {
            var eqDomains = new List<List<string>>();
            if (response != null && response.EquivalentDomains != null)
            {
                eqDomains = eqDomains.Concat(response.EquivalentDomains).ToList();
            }
            if (response != null && response.GlobalEquivalentDomains != null)
            {
                foreach (var global in response.GlobalEquivalentDomains)
                {
                    if (global.Domains.Any())
                    {
                        eqDomains.Add(global.Domains);
                    }
                }
            }
            await _settingsService.SetEquivalentDomainsAsync(eqDomains);
        }

        private async Task SyncPoliciesAsync(List<PolicyResponse> response)
        {
            var policies = response?.ToDictionary(p => p.Id, p => new PolicyData(p)) ??
                new Dictionary<string, PolicyData>();
            await _policyService.Replace(policies);
        }

        private async Task SyncSendsAsync(string userId, List<SendResponse> response)
        {
            var sends = response?.ToDictionary(s => s.Id, s => new SendData(s, userId)) ?? 
                new Dictionary<string, SendData>();
            await _sendService.ReplaceAsync(sends);
        }
    }
}
