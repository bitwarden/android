using System;
using System.Net.Http;
using System.Threading.Tasks;
using Bit.Core.Models.Domain;

namespace Bit.Core.Services
{
    public class BitwardenFileUploadService
    {
        public BitwardenFileUploadService(ApiService apiService)
        {
            _apiService = apiService;
        }

        private readonly ApiService _apiService;

        public async Task Upload(string encryptedFileName, EncByteArray encryptedFileData, Func<MultipartFormDataContent, Task> apiCall)
        {
            var fd = new MultipartFormDataContent($"--BWMobileFormBoundary{DateTime.UtcNow.Ticks}")
            {
                { new ByteArrayContent(encryptedFileData.Buffer), "data", encryptedFileName }
            };

            await apiCall(fd);
        }
    }
}
