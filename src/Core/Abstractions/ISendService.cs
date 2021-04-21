using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.Core.Models.Data;
using Bit.Core.Models.Domain;
using Bit.Core.Models.View;

namespace Bit.Core.Abstractions
{
    public interface ISendService
    {
        void ClearCache();
        Task<(Send send, EncByteArray encryptedFileData)> EncryptAsync(SendView model, byte[] fileData, string password,
            SymmetricCryptoKey key = null);
        Task<Send> GetAsync(string id);
        Task<List<Send>> GetAllAsync();
        Task<List<SendView>> GetAllDecryptedAsync();
        Task<string> SaveWithServerAsync(Send sendData, EncByteArray encryptedFileData);
        Task UpsertAsync(params SendData[] send);
        Task ReplaceAsync(Dictionary<string, SendData> sends);
        Task ClearAsync(string userId);
        Task DeleteAsync(params string[] ids);
        Task DeleteWithServerAsync(string id);
        Task RemovePasswordWithServerAsync(string id);
    }
}
