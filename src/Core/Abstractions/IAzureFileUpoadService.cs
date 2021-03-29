using System;
using System.Threading.Tasks;

namespace Bit.Core.Abstractions
{
    public interface IAzureFileUploadService
    {
        Task Upload(string uri, byte[] data, Func<Task<string>> renewalCallback);
    }
}
