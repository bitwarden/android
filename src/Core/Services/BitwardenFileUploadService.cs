using System;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Net.Mime;
using System.Threading;
using System.Threading.Tasks;
using Bit.Core.Models.Domain;

namespace Bit.Core.Services
{
    public class BitwardenFileUploadService
    {
        private readonly ApiService _apiService;

        public BitwardenFileUploadService(ApiService apiService)
        {
            _apiService = apiService;
        }

        public async Task Upload(string encryptedFileName, EncByteArray encryptedFileData, Func<MultipartFormDataContent, CancellationToken, Task> apiCall, CancellationToken cancellationToken)
        {
            var fd = new MultipartFormDataContent($"--BWMobileFormBoundary{DateTime.UtcNow.Ticks}")
            {
                { new ByteArrayContent(encryptedFileData.Buffer) { Headers = { ContentType = new MediaTypeHeaderValue(MediaTypeNames.Application.Octet) } }, "data", encryptedFileName }
            };

            await apiCall(fd, cancellationToken);
        }
    }
}
