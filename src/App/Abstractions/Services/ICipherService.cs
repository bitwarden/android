using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.App.Models;
using Bit.App.Models.Api;
using System;

namespace Bit.App.Abstractions
{
    public interface ICipherService
    {
        Task<Cipher> GetByIdAsync(string id);
        Task<IEnumerable<Cipher>> GetAllAsync();
        Task<IEnumerable<Cipher>> GetAllAsync(bool favorites);
        Task<Tuple<IEnumerable<Cipher>, IEnumerable<Cipher>>> GetAllAsync(string uriString);
        Task<ApiResult<CipherResponse>> SaveAsync(Cipher login);
        Task<ApiResult> DeleteAsync(string id);
        Task<byte[]> DownloadAndDecryptAttachmentAsync(string url, string orgId = null);
        Task<ApiResult<CipherResponse>> EncryptAndSaveAttachmentAsync(Cipher login, byte[] data, string fileName);
        Task<ApiResult> DeleteAttachmentAsync(Cipher login, string attachmentId);
    }
}
