using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.Core.Models.Data;
using Bit.Core.Models.Domain;
using Bit.Core.Models.View;

namespace Bit.Core.Abstractions
{
    public interface ICollectionService
    {
        Task ClearAsync(string userId);
        void ClearCache();
        Task<List<CollectionView>> DecryptManyAsync(List<Collection> collections);
        Task DeleteAsync(string id);
        Task<Collection> EncryptAsync(CollectionView model);
        Task<List<Collection>> GetAllAsync();
        Task<List<CollectionView>> GetAllDecryptedAsync();
        Task<List<TreeNode<CollectionView>>> GetAllNestedAsync(List<CollectionView> collections = null);
        Task<Collection> GetAsync(string id);
        Task<TreeNode<CollectionView>> GetNestedAsync(string id);
        Task ReplaceAsync(Dictionary<string, CollectionData> collections);
        Task UpsertAsync(CollectionData collection);
        Task UpsertAsync(List<CollectionData> collection);
    }
}
