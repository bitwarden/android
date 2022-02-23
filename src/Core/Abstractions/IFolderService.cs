using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.Core.Models.Data;
using Bit.Core.Models.Domain;
using Bit.Core.Models.View;

namespace Bit.Core.Abstractions
{
    public interface IFolderService
    {
        Task ClearAsync(string userId);
        void ClearCache();
        Task DeleteAsync(string id);
        Task DeleteWithServerAsync(string id);
        Task<Folder> EncryptAsync(FolderView model, SymmetricCryptoKey key = null);
        Task<List<Folder>> GetAllAsync();
        Task<List<FolderView>> GetAllDecryptedAsync();
        Task<List<TreeNode<FolderView>>> GetAllNestedAsync();
        Task<Folder> GetAsync(string id);
        Task<TreeNode<FolderView>> GetNestedAsync(string id);
        Task ReplaceAsync(Dictionary<string, FolderData> folders);
        Task SaveWithServerAsync(Folder folder);
        Task UpsertAsync(FolderData folder);
        Task UpsertAsync(List<FolderData> folder);
    }
}
