using System;
using System.Collections.Generic;
using System.Linq;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Models.Data;
using Bit.Core.Models.Domain;
using Bit.Core.Models.View;
using Bit.Core.Utilities;

namespace Bit.Core.Services
{
    public class CollectionService : ICollectionService
    {
        private const char NestingDelimiter = '/';

        private List<CollectionView> _decryptedCollectionCache;
        private readonly ICryptoService _cryptoService;
        private readonly IStateService _stateService;
        private readonly II18nService _i18nService;

        public CollectionService(
            ICryptoService cryptoService,
            IStateService stateService,
            II18nService i18nService)
        {
            _cryptoService = cryptoService;
            _stateService = stateService;
            _i18nService = i18nService;
        }

        public void ClearCache()
        {
            _decryptedCollectionCache = null;
        }

        public async Task<Collection> EncryptAsync(CollectionView model)
        {
            if (model.OrganizationId == null)
            {
                throw new Exception("Collection has no organization id.");
            }
            var key = await _cryptoService.GetOrgKeyAsync(model.OrganizationId);
            if (key == null)
            {
                throw new Exception("No key for this collection's organization.");
            }
            var collection = new Collection
            {
                Id = model.Id,
                OrganizationId = model.OrganizationId,
                ReadOnly = model.ReadOnly,
                Name = await _cryptoService.EncryptAsync(model.Name, key)
            };
            return collection;
        }

        public async Task<List<CollectionView>> DecryptManyAsync(List<Collection> collections)
        {
            if (collections == null)
            {
                return new List<CollectionView>();
            }
            var decCollections = new List<CollectionView>();
            async Task decryptAndAddCollectionAsync(Collection collection)
            {
                var c = await collection.DecryptAsync();
                decCollections.Add(c);
            }
            var tasks = new List<Task>();
            foreach (var collection in collections)
            {
                tasks.Add(decryptAndAddCollectionAsync(collection));
            }
            await Task.WhenAll(tasks);
            return decCollections.OrderBy(c => c, new CollectionLocaleComparer(_i18nService)).ToList();
        }

        public async Task<Collection> GetAsync(string id)
        {
            var collections = await _stateService.GetEncryptedCollectionsAsync();
            if (!collections?.ContainsKey(id) ?? true)
            {
                return null;
            }
            return new Collection(collections[id]);
        }

        public async Task<List<Collection>> GetAllAsync()
        {
            var collections = await _stateService.GetEncryptedCollectionsAsync();
            var response = collections?.Select(c => new Collection(c.Value));
            return response?.ToList() ?? new List<Collection>();
        }

        // TODO: sequentialize?
        public async Task<List<CollectionView>> GetAllDecryptedAsync()
        {
            if (_decryptedCollectionCache != null)
            {
                return _decryptedCollectionCache;
            }
            var hasKey = await _cryptoService.HasUserKeyAsync();
            if (!hasKey)
            {
                throw new Exception("No key.");
            }
            var collections = await GetAllAsync();
            _decryptedCollectionCache = await DecryptManyAsync(collections);
            return _decryptedCollectionCache;
        }

        public async Task<List<TreeNode<CollectionView>>> GetAllNestedAsync(List<CollectionView> collections = null)
        {
            if (collections == null)
            {
                collections = await GetAllDecryptedAsync();
            }
            var nodes = new List<TreeNode<CollectionView>>();
            foreach (var c in collections)
            {
                var collectionCopy = new CollectionView
                {
                    Id = c.Id,
                    OrganizationId = c.OrganizationId
                };
                var parts = c.Name != null ?
                    Regex.Replace(c.Name, "^\\/+|\\/+$", string.Empty).Split(NestingDelimiter) : new string[] { };
                CoreHelpers.NestedTraverse(nodes, 0, parts, collectionCopy, null, NestingDelimiter);
            }
            return nodes;
        }

        public async Task<TreeNode<CollectionView>> GetNestedAsync(string id)
        {
            var collections = await GetAllNestedAsync();
            return CoreHelpers.GetTreeNodeObject(collections, id);
        }

        public async Task UpsertAsync(CollectionData collection)
        {
            var collections = await _stateService.GetEncryptedCollectionsAsync();
            if (collections == null)
            {
                collections = new Dictionary<string, CollectionData>();
            }
            if (!collections.ContainsKey(collection.Id))
            {
                collections.Add(collection.Id, null);
            }
            collections[collection.Id] = collection;
            await _stateService.SetEncryptedCollectionsAsync(collections);
            _decryptedCollectionCache = null;
        }

        public async Task UpsertAsync(List<CollectionData> collection)
        {
            var collections = await _stateService.GetEncryptedCollectionsAsync();
            if (collections == null)
            {
                collections = new Dictionary<string, CollectionData>();
            }
            foreach (var c in collection)
            {
                if (!collections.ContainsKey(c.Id))
                {
                    collections.Add(c.Id, null);
                }
                collections[c.Id] = c;
            }
            await _stateService.SetEncryptedCollectionsAsync(collections);
            _decryptedCollectionCache = null;
        }

        public async Task ReplaceAsync(Dictionary<string, CollectionData> collections)
        {
            await _stateService.SetEncryptedCollectionsAsync(collections);
            _decryptedCollectionCache = null;
        }

        public async Task ClearAsync(string userId)
        {
            await _stateService.SetEncryptedCollectionsAsync(null, userId);
            _decryptedCollectionCache = null;
        }

        public async Task DeleteAsync(string id)
        {
            var collections = await _stateService.GetEncryptedCollectionsAsync();
            if (collections == null || !collections.ContainsKey(id))
            {
                return;
            }
            collections.Remove(id);
            await _stateService.SetEncryptedCollectionsAsync(collections);
            _decryptedCollectionCache = null;
        }

        private class CollectionLocaleComparer : IComparer<CollectionView>
        {
            private readonly II18nService _i18nService;

            public CollectionLocaleComparer(II18nService i18nService)
            {
                _i18nService = i18nService;
            }

            public int Compare(CollectionView a, CollectionView b)
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
