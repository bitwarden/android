using System;
using System.Threading.Tasks;

namespace Bit.Core.Abstractions
{
    public interface IAzureStorageService
    {
        Task UploadFileToServerAsync(string uri, byte[] data, Func<Task<string>> renewalCallback);
    }
}
