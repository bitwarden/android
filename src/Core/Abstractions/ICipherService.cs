using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.Core.Enums;
using Bit.Core.Models.Data;
using Bit.Core.Models.Domain;
using Bit.Core.Models.View;

namespace Bit.Core.Abstractions
{
    public interface ICipherService
    {
        Task ClearAsync(string userId);
        Task ClearCacheAsync();
        Task DeleteAsync(List<string> ids);
        Task DeleteAsync(string id);
        Task DeleteAttachmentAsync(string id, string attachmentId);
        Task DeleteAttachmentWithServerAsync(string id, string attachmentId);
        Task DeleteWithServerAsync(string id);
        Task<Cipher> EncryptAsync(CipherView model, SymmetricCryptoKey key = null, Cipher originalCipher = null);
        Task<List<Cipher>> GetAllAsync();
        Task<List<CipherView>> GetAllDecryptedAsync();
        Task<Tuple<List<CipherView>, List<CipherView>, List<CipherView>>> GetAllDecryptedByUrlAsync(string url, 
            List<CipherType> includeOtherTypes = null);
        Task<List<CipherView>> GetAllDecryptedForGroupingAsync(string groupingId, bool folder = true);
        Task<List<CipherView>> GetAllDecryptedForUrlAsync(string url);
        Task<Cipher> GetAsync(string id);
        Task<CipherView> GetLastUsedForUrlAsync(string url);
        Task ReplaceAsync(Dictionary<string, CipherData> ciphers);
        Task<Cipher> SaveAttachmentRawWithServerAsync(Cipher cipher, string filename, byte[] data);
        Task SaveCollectionsWithServerAsync(Cipher cipher);
        Task SaveNeverDomainAsync(string domain);
        Task SaveWithServerAsync(Cipher cipher);
        Task ShareWithServerAsync(CipherView cipher, string organizationId, HashSet<string> collectionIds);
        Task UpdateLastUsedDateAsync(string id);
        Task UpsertAsync(CipherData cipher);
        Task UpsertAsync(List<CipherData> cipher);
        Task<byte[]> DownloadAndDecryptAttachmentAsync(string cipherId, AttachmentView attachment, string organizationId);
        Task SoftDeleteWithServerAsync(string id);
        Task RestoreWithServerAsync(string id);
    }
}
