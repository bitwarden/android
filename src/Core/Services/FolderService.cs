using System;
using System.Collections.Generic;
using System.Linq;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Exceptions;
using Bit.Core.Models.Data;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Request;
using Bit.Core.Models.Response;
using Bit.Core.Models.View;
using Bit.Core.Utilities;

namespace Bit.Core.Services
{
    public class FolderService : IFolderService
    {
        private const char NestingDelimiter = '/';

        private List<FolderView> _decryptedFolderCache;
        private readonly ICryptoService _cryptoService;
        private readonly IStateService _stateService;
        private readonly IApiService _apiService;
        private readonly II18nService _i18nService;
        private readonly ICipherService _cipherService;

        public FolderService(
            ICryptoService cryptoService,
            IStateService stateService,
            IApiService apiService,
            II18nService i18nService,
            ICipherService cipherService)
        {
            _cryptoService = cryptoService;
            _stateService = stateService;
            _apiService = apiService;
            _i18nService = i18nService;
            _cipherService = cipherService;
        }

        public void ClearCache()
        {
            _decryptedFolderCache = null;
        }

        public async Task<Folder> EncryptAsync(FolderView model, SymmetricCryptoKey key = null)
        {
            var folder = new Folder
            {
                Id = model.Id,
                Name = await _cryptoService.EncryptAsync(model.Name, key)
            };
            return folder;
        }

        public async Task<Folder> GetAsync(string id)
        {
            var folders = await _stateService.GetEncryptedFoldersAsync();
            if (!folders?.ContainsKey(id) ?? true)
            {
                return null;
            }
            return new Folder(folders[id]);
        }

        public async Task<List<Folder>> GetAllAsync()
        {
            var folders = await _stateService.GetEncryptedFoldersAsync();
            var response = folders?.Select(f => new Folder(f.Value));
            return response?.ToList() ?? new List<Folder>();
        }

        // TODO: sequentialize?
        public async Task<List<FolderView>> GetAllDecryptedAsync()
        {
            if (_decryptedFolderCache != null)
            {
                return _decryptedFolderCache;
            }
            var hasKey = await _cryptoService.HasUserKeyAsync();
            if (!hasKey)
            {
                throw new UserKeyNullException();
            }
            var decFolders = new List<FolderView>();
            async Task decryptAndAddFolderAsync(Folder folder)
            {
                var f = await folder.DecryptAsync();
                decFolders.Add(f);
            }
            var tasks = new List<Task>();
            var folders = await GetAllAsync();
            foreach (var folder in folders)
            {
                tasks.Add(decryptAndAddFolderAsync(folder));
            }
            await Task.WhenAll(tasks);
            decFolders = decFolders.OrderBy(f => f, new FolderLocaleComparer(_i18nService)).ToList();

            var noneFolder = new FolderView
            {
                Name = _i18nService.T("FolderNone")
            };
            decFolders.Add(noneFolder);

            _decryptedFolderCache = decFolders;
            return _decryptedFolderCache;
        }

        public async Task<List<TreeNode<FolderView>>> GetAllNestedAsync(List<FolderView> folders = null)
        {
            if (folders == null)
            {
                folders = await GetAllDecryptedAsync();
            }
            var nodes = new List<TreeNode<FolderView>>();
            foreach (var f in folders)
            {
                var folderCopy = new FolderView
                {
                    Id = f.Id,
                    RevisionDate = f.RevisionDate
                };
                var parts = f.Name != null ?
                    Regex.Replace(f.Name, "^\\/+|\\/+$", string.Empty).Split(NestingDelimiter) : new string[] { };
                CoreHelpers.NestedTraverse(nodes, 0, parts, folderCopy, null, NestingDelimiter);
            }
            return nodes;
        }

        public async Task<TreeNode<FolderView>> GetNestedAsync(string id)
        {
            var folders = await GetAllNestedAsync();
            return CoreHelpers.GetTreeNodeObject(folders, id);
        }

        public async Task SaveWithServerAsync(Folder folder)
        {
            var request = new FolderRequest(folder);
            FolderResponse response;
            if (folder.Id == null)
            {
                response = await _apiService.PostFolderAsync(request);
                folder.Id = response.Id;
            }
            else
            {
                response = await _apiService.PutFolderAsync(folder.Id, request);
            }
            var userId = await _stateService.GetActiveUserIdAsync();
            var data = new FolderData(response, userId);
            await UpsertAsync(data);
        }

        public async Task UpsertAsync(FolderData folder)
        {
            var folders = await _stateService.GetEncryptedFoldersAsync();
            if (folders == null)
            {
                folders = new Dictionary<string, FolderData>();
            }
            if (!folders.ContainsKey(folder.Id))
            {
                folders.Add(folder.Id, null);
            }
            folders[folder.Id] = folder;
            await _stateService.SetEncryptedFoldersAsync(folders);
            _decryptedFolderCache = null;
        }

        public async Task UpsertAsync(List<FolderData> folder)
        {
            var folders = await _stateService.GetEncryptedFoldersAsync();
            if (folders == null)
            {
                folders = new Dictionary<string, FolderData>();
            }
            foreach (var f in folder)
            {
                if (!folders.ContainsKey(f.Id))
                {
                    folders.Add(f.Id, null);
                }
                folders[f.Id] = f;
            }
            await _stateService.SetEncryptedFoldersAsync(folders);
            _decryptedFolderCache = null;
        }

        public async Task ReplaceAsync(Dictionary<string, FolderData> folders)
        {
            await _stateService.SetEncryptedFoldersAsync(folders);
            _decryptedFolderCache = null;
        }

        public async Task ClearAsync(string userId)
        {
            await _stateService.SetEncryptedFoldersAsync(null, userId);
            _decryptedFolderCache = null;
        }

        public async Task DeleteAsync(string id)
        {
            var folders = await _stateService.GetEncryptedFoldersAsync();
            if (folders == null || !folders.ContainsKey(id))
            {
                return;
            }
            folders.Remove(id);
            await _stateService.SetEncryptedFoldersAsync(folders);
            _decryptedFolderCache = null;

            // Items in a deleted folder are re-assigned to "No Folder"
            var ciphers = await _stateService.GetEncryptedCiphersAsync();
            if (ciphers != null)
            {
                var updates = new List<CipherData>();
                foreach (var c in ciphers)
                {
                    if (c.Value.FolderId == id)
                    {
                        c.Value.FolderId = null;
                        updates.Add(c.Value);
                    }
                }
                if (updates.Any())
                {
                    await _cipherService.UpsertAsync(updates);
                }
            }
        }

        public async Task DeleteWithServerAsync(string id)
        {
            await _apiService.DeleteFolderAsync(id);
            await DeleteAsync(id);
        }

        private class FolderLocaleComparer : IComparer<FolderView>
        {
            private readonly II18nService _i18nService;

            public FolderLocaleComparer(II18nService i18nService)
            {
                _i18nService = i18nService;
            }

            public int Compare(FolderView a, FolderView b)
            {
                var aName = a?.Name;
                var bName = b?.Name;
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
    }
}
