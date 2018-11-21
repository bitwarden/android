using System;
using System.Threading.Tasks;
using Bit.App.Models.Api;

namespace Bit.App.Abstractions
{
    public interface ICipherApiRepository : IApiRepository<CipherRequest, CipherResponse, string>
    {
        Task<ApiResult<CipherResponse>> PostAttachmentAsync(string cipherId, byte[] data, string key, string fileName);
        Task<ApiResult> DeleteAttachmentAsync(string cipherId, string attachmentId);
    }
}