using System;
using System.Net.Http;
using System.Threading.Tasks;

namespace Bit.Core.Services
{
    public class BitwardenFileUploadService
    {
        public BitwardenFileUploadService(ApiService apiService)
        {
            _apiService = apiService;
        }

        private readonly ApiService _apiService;

        public async Task Upload(string encryptedFileName, byte[] encryptedFileData, Func<MultipartFormDataContent, Task> apiCall)
        {
            var fd = new MultipartFormDataContent($"--BWMobileFormBoundary{DateTime.UtcNow.Ticks}")
            {
                { new ByteArrayContent(encryptedFileData), "data", encryptedFileName }
            };

            await apiCall(fd);
        }
    }
}
