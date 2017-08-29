using System;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models.Data;
using Plugin.Settings.Abstractions;
using Bit.App.Models.Api;
using System.Collections.Generic;
using Xamarin.Forms;
using Newtonsoft.Json;
using Bit.App.Models;

namespace Bit.App.Services
{
    public class SyncService : ISyncService
    {
        private readonly ICipherApiRepository _cipherApiRepository;
        private readonly IFolderApiRepository _folderApiRepository;
        private readonly ILoginApiRepository _loginApiRepository;
        private readonly IAccountsApiRepository _accountsApiRepository;
        private readonly ISettingsApiRepository _settingsApiRepository;
        private readonly IFolderRepository _folderRepository;
        private readonly ILoginRepository _loginRepository;
        private readonly IAttachmentRepository _attachmentRepository;
        private readonly ISettingsRepository _settingsRepository;
        private readonly IAuthService _authService;
        private readonly ICryptoService _cryptoService;
        private readonly ISettings _settings;
        private readonly IAppSettingsService _appSettingsService;

        public SyncService(
            ICipherApiRepository cipherApiRepository,
            IFolderApiRepository folderApiRepository,
            ILoginApiRepository loginApiRepository,
            IAccountsApiRepository accountsApiRepository,
            ISettingsApiRepository settingsApiRepository,
            IFolderRepository folderRepository,
            ILoginRepository loginRepository,
            IAttachmentRepository attachmentRepository,
            ISettingsRepository settingsRepository,
            IAuthService authService,
            ICryptoService cryptoService,
            ISettings settings,
            IAppSettingsService appSettingsService)
        {
            _cipherApiRepository = cipherApiRepository;
            _folderApiRepository = folderApiRepository;
            _loginApiRepository = loginApiRepository;
            _accountsApiRepository = accountsApiRepository;
            _settingsApiRepository = settingsApiRepository;
            _folderRepository = folderRepository;
            _loginRepository = loginRepository;
            _attachmentRepository = attachmentRepository;
            _settingsRepository = settingsRepository;
            _authService = authService;
            _cryptoService = cryptoService;
            _settings = settings;
            _appSettingsService = appSettingsService;
        }

        public bool SyncInProgress { get; private set; }

        public async Task<bool> SyncCipherAsync(string id)
        {
            if(!_authService.IsAuthenticated)
            {
                return false;
            }

            SyncStarted();

            var cipher = await _cipherApiRepository.GetByIdAsync(id).ConfigureAwait(false);
            if(!CheckSuccess(cipher))
            {
                return false;
            }

            try
            {
                switch(cipher.Result.Type)
                {
                    case Enums.CipherType.Login:
                        var loginData = new LoginData(cipher.Result, _authService.UserId);
                        await _loginRepository.UpsertAsync(loginData).ConfigureAwait(false);

                        var localAttachments = (await _attachmentRepository.GetAllByLoginIdAsync(loginData.Id)
                            .ConfigureAwait(false));

                        if(cipher.Result.Attachments != null)
                        {
                            foreach(var attachment in cipher.Result.Attachments)
                            {
                                var attachmentData = new AttachmentData(attachment, loginData.Id);
                                await _attachmentRepository.UpsertAsync(attachmentData).ConfigureAwait(false);
                            }
                        }

                        if(localAttachments != null)
                        {
                            foreach(var attachment in localAttachments
                                .Where(a => !cipher.Result.Attachments.Any(sa => sa.Id == a.Id)))
                            {
                                try
                                {
                                    await _attachmentRepository.DeleteAsync(attachment.Id).ConfigureAwait(false);
                                }
                                catch(SQLite.SQLiteException) { }
                            }
                        }
                        break;
                    default:
                        SyncCompleted(false);
                        return false;
                }
            }
            catch(SQLite.SQLiteException)
            {
                SyncCompleted(false);
                return false;
            }

            SyncCompleted(true);
            return true;
        }

        public async Task<bool> SyncFolderAsync(string id)
        {
            if(!_authService.IsAuthenticated)
            {
                return false;
            }

            SyncStarted();

            var folder = await _folderApiRepository.GetByIdAsync(id).ConfigureAwait(false);
            if(!CheckSuccess(folder))
            {
                return false;
            }

            try
            {
                var folderData = new FolderData(folder.Result, _authService.UserId);
                await _folderRepository.UpsertAsync(folderData).ConfigureAwait(false);
            }
            catch(SQLite.SQLiteException)
            {
                SyncCompleted(false);
                return false;
            }

            SyncCompleted(true);
            return true;
        }

        public async Task<bool> SyncDeleteFolderAsync(string id, DateTime revisionDate)
        {
            if(!_authService.IsAuthenticated)
            {
                return false;
            }

            SyncStarted();

            try
            {
                await _folderRepository.DeleteWithLoginUpdateAsync(id, revisionDate).ConfigureAwait(false);
                SyncCompleted(true);
                return true;
            }
            catch(SQLite.SQLiteException)
            {
                SyncCompleted(false);
                return false;
            }
        }

        public async Task<bool> SyncDeleteLoginAsync(string id)
        {
            if(!_authService.IsAuthenticated)
            {
                return false;
            }

            SyncStarted();

            try
            {
                await _loginRepository.DeleteAsync(id).ConfigureAwait(false);
                SyncCompleted(true);
                return true;
            }
            catch(SQLite.SQLiteException)
            {
                SyncCompleted(false);
                return false;
            }
        }

        public async Task<bool> SyncSettingsAsync()
        {
            if(!_authService.IsAuthenticated)
            {
                return false;
            }

            SyncStarted();

            var domains = await _settingsApiRepository.GetDomains(false).ConfigureAwait(false);
            if(!CheckSuccess(domains))
            {
                return false;
            }

            await SyncDomainsAsync(domains.Result);

            SyncCompleted(true);
            return true;
        }

        public async Task<bool> SyncProfileAsync()
        {
            if(!_authService.IsAuthenticated)
            {
                return false;
            }

            SyncStarted();

            var profile = await _accountsApiRepository.GetProfileAsync().ConfigureAwait(false);
            if(!CheckSuccess(profile, !string.IsNullOrWhiteSpace(_appSettingsService.SecurityStamp) &&
                _appSettingsService.SecurityStamp != profile.Result.SecurityStamp))
            {
                return false;
            }

            await SyncProfileKeysAsync(profile.Result);

            SyncCompleted(true);
            return true;
        }

        public async Task<bool> FullSyncAsync(TimeSpan syncThreshold, bool forceSync = false)
        {
            var lastSync = _settings.GetValueOrDefault(Constants.LastSync, DateTime.MinValue);
            if(DateTime.UtcNow - lastSync < syncThreshold)
            {
                return false;
            }

            return await FullSyncAsync(forceSync).ConfigureAwait(false);
        }

        public async Task<bool> FullSyncAsync(bool forceSync = false)
        {
            if(!_authService.IsAuthenticated)
            {
                return false;
            }

            if(!forceSync && !(await NeedsToSyncAsync()))
            {
                _settings.AddOrUpdateValue(Constants.LastSync, DateTime.UtcNow);
                return false;
            }

            SyncStarted();

            var now = DateTime.UtcNow;

            // Just check profile first to make sure we'll have a success with the API
            var profile = await _accountsApiRepository.GetProfileAsync().ConfigureAwait(false);
            if(!CheckSuccess(profile, !string.IsNullOrWhiteSpace(_appSettingsService.SecurityStamp) &&
                _appSettingsService.SecurityStamp != profile.Result.SecurityStamp))
            {
                return false;
            }

            var ciphers = await _cipherApiRepository.GetAsync().ConfigureAwait(false);
            var folders = await _folderApiRepository.GetAsync().ConfigureAwait(false);
            var domains = await _settingsApiRepository.GetDomains(false).ConfigureAwait(false);
            if(!CheckSuccess(ciphers) || !CheckSuccess(folders) || !CheckSuccess(domains))
            {
                return false;
            }

            var loginsDict = ciphers.Result.Data.Where(c => c.Type == Enums.CipherType.Login).ToDictionary(s => s.Id);
            var foldersDict = folders.Result.Data.ToDictionary(f => f.Id);

            var loginTask = SyncLoginsAsync(loginsDict);
            var folderTask = SyncFoldersAsync(foldersDict);
            var domainsTask = SyncDomainsAsync(domains.Result);
            var profileTask = SyncProfileKeysAsync(profile.Result);
            await Task.WhenAll(loginTask, folderTask, domainsTask, profileTask).ConfigureAwait(false);

            if(folderTask.Exception != null || loginTask.Exception != null || domainsTask.Exception != null ||
                profileTask.Exception != null)
            {
                SyncCompleted(false);
                return false;
            }

            _settings.AddOrUpdateValue(Constants.LastSync, now);
            SyncCompleted(true);
            return true;
        }

        private async Task<bool> NeedsToSyncAsync()
        {
            if(!_settings.Contains(Constants.LastSync))
            {
                return true;
            }
            var lastSync = _settings.GetValueOrDefault(Constants.LastSync, DateTime.MinValue);

            var accountRevisionDate = await _accountsApiRepository.GetAccountRevisionDateAsync();
            if(accountRevisionDate.Succeeded && accountRevisionDate.Result.HasValue &&
                accountRevisionDate.Result.Value > lastSync)
            {
                return true;
            }

            if(Application.Current != null && (accountRevisionDate.StatusCode == System.Net.HttpStatusCode.Forbidden
                || accountRevisionDate.StatusCode == System.Net.HttpStatusCode.Unauthorized))
            {
                MessagingCenter.Send(Application.Current, "Logout", (string)null);
            }

            return false;
        }

        private async Task SyncFoldersAsync(IDictionary<string, FolderResponse> serverFolders)
        {
            if(!_authService.IsAuthenticated)
            {
                return;
            }

            var localFolders = (await _folderRepository.GetAllByUserIdAsync(_authService.UserId)
                .ConfigureAwait(false))
                .GroupBy(f => f.Id)
                .Select(f => f.First())
                .ToDictionary(f => f.Id);

            foreach(var serverFolder in serverFolders)
            {
                if(!_authService.IsAuthenticated)
                {
                    return;
                }

                try
                {
                    var data = new FolderData(serverFolder.Value, _authService.UserId);
                    await _folderRepository.UpsertAsync(data).ConfigureAwait(false);
                }
                catch(SQLite.SQLiteException) { }
            }

            foreach(var folder in localFolders.Where(localFolder => !serverFolders.ContainsKey(localFolder.Key)))
            {
                try
                {
                    await _folderRepository.DeleteAsync(folder.Value.Id).ConfigureAwait(false);
                }
                catch(SQLite.SQLiteException) { }
            }
        }

        private async Task SyncLoginsAsync(IDictionary<string, CipherResponse> serverLogins)
        {
            if(!_authService.IsAuthenticated)
            {
                return;
            }

            var localLogins = (await _loginRepository.GetAllByUserIdAsync(_authService.UserId)
                .ConfigureAwait(false))
                .GroupBy(s => s.Id)
                .Select(s => s.First())
                .ToDictionary(s => s.Id);

            var localAttachments = (await _attachmentRepository.GetAllByUserIdAsync(_authService.UserId)
                .ConfigureAwait(false))
                .GroupBy(a => a.LoginId)
                .ToDictionary(g => g.Key);

            foreach(var serverLogin in serverLogins)
            {
                if(!_authService.IsAuthenticated)
                {
                    return;
                }

                try
                {
                    var localLogin = localLogins.ContainsKey(serverLogin.Value.Id) ? localLogins[serverLogin.Value.Id] : null;

                    var data = new LoginData(serverLogin.Value, _authService.UserId);
                    await _loginRepository.UpsertAsync(data).ConfigureAwait(false);

                    if(serverLogin.Value.Attachments != null)
                    {
                        foreach(var attachment in serverLogin.Value.Attachments)
                        {
                            var attachmentData = new AttachmentData(attachment, data.Id);
                            await _attachmentRepository.UpsertAsync(attachmentData).ConfigureAwait(false);
                        }
                    }

                    if(localLogin != null && localAttachments != null && localAttachments.ContainsKey(localLogin.Id))
                    {
                        foreach(var attachment in localAttachments[localLogin.Id]
                            .Where(a => !serverLogin.Value.Attachments.Any(sa => sa.Id == a.Id)))
                        {
                            try
                            {
                                await _attachmentRepository.DeleteAsync(attachment.Id).ConfigureAwait(false);
                            }
                            catch(SQLite.SQLiteException) { }
                        }
                    }
                }
                catch(SQLite.SQLiteException) { }
            }

            foreach(var login in localLogins.Where(localLogin => !serverLogins.ContainsKey(localLogin.Key)))
            {
                try
                {
                    await _loginRepository.DeleteAsync(login.Value.Id).ConfigureAwait(false);
                }
                catch(SQLite.SQLiteException) { }
            }
        }

        private async Task SyncDomainsAsync(DomainsResponse serverDomains)
        {
            var eqDomains = new List<IEnumerable<string>>();
            if(serverDomains.EquivalentDomains != null)
            {
                eqDomains.AddRange(serverDomains.EquivalentDomains);
            }

            if(serverDomains.GlobalEquivalentDomains != null)
            {
                eqDomains.AddRange(serverDomains.GlobalEquivalentDomains.Select(d => d.Domains));
            }

            try
            {
                await _settingsRepository.UpsertAsync(new SettingsData
                {
                    Id = _authService.UserId,
                    EquivalentDomains = JsonConvert.SerializeObject(eqDomains)
                });
            }
            catch(SQLite.SQLiteException) { }
        }

        private Task SyncProfileKeysAsync(ProfileResponse profile)
        {
            if(!string.IsNullOrWhiteSpace(profile.Key))
            {
                _cryptoService.SetEncKey(new CipherString(profile.Key));
            }

            if(!string.IsNullOrWhiteSpace(profile.PrivateKey))
            {
                _cryptoService.SetPrivateKey(new CipherString(profile.PrivateKey));
            }

            if(!string.IsNullOrWhiteSpace(profile.SecurityStamp))
            {
                _appSettingsService.SecurityStamp = profile.SecurityStamp;
            }

            _cryptoService.SetOrgKeys(profile);
            return Task.FromResult(0);
        }

        private void SyncStarted()
        {
            if(Application.Current == null)
            {
                return;
            }

            SyncInProgress = true;
            MessagingCenter.Send(Application.Current, "SyncStarted");
        }

        private void SyncCompleted(bool successfully)
        {
            if(Application.Current == null)
            {
                return;
            }

            SyncInProgress = false;
            MessagingCenter.Send(Application.Current, "SyncCompleted", successfully);
        }

        private bool CheckSuccess<T>(ApiResult<T> result, bool logout = false)
        {
            if(!result.Succeeded || logout)
            {
                SyncCompleted(false);

                if(Application.Current != null && (logout ||
                    result.StatusCode == System.Net.HttpStatusCode.Forbidden ||
                    result.StatusCode == System.Net.HttpStatusCode.Unauthorized))
                {
                    MessagingCenter.Send(Application.Current, "Logout", (string)null);
                }

                return false;
            }

            return true;
        }
    }
}
