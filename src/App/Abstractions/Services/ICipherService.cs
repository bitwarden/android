using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.App.Models;
using Bit.App.Models.Api;
using System;
using Bit.App.Models.Data;

namespace Bit.App.Abstractions
{
    public interface ICipherService
    {
        Task<Cipher> GetByIdAsync(string id);
        Task<IEnumerable<Cipher>> GetAllAsync();
        Task<IEnumerable<Cipher>> GetAllAsync(bool favorites);
        Task<IEnumerable<Cipher>> GetAllByFolderAsync(string folderId);
        Task<IEnumerable<Cipher>> GetAllByCollectionAsync(string collectionId);
        Task<Tuple<IEnumerable<Cipher>, IEnumerable<Cipher>, IEnumerable<Cipher>>> GetAllAsync(string uriString);
        Task<ApiResult<CipherResponse>> SaveAsync(Cipher cipher);
        Task UpsertDataAsync(CipherData cipher, bool sendMessage, bool created);
        Task<ApiResult> DeleteAsync(string id);
        Task DeleteDataAsync(string id, bool sendMessage);
        Task<byte[]> DownloadAndDecryptAttachmentAsync(string url, CipherString key, string orgId = null);
        Task<ApiResult<CipherResponse>> EncryptAndSaveAttachmentAsync(Cipher cipher, byte[] data, string fileName);
        Task UpsertAttachmentDataAsync(IEnumerable<AttachmentData> attachments);
        Task<ApiResult> DeleteAttachmentAsync(Cipher cipher, string attachmentId);
        Task DeleteAttachmentDataAsync(string attachmentId);
    }
}
