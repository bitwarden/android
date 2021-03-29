using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Response;
using Bit.Core.Enums;
using System;

namespace Bit.Core.Services {
    public class FileUploadService : IFileUploadService
    {
        public FileUploadService(ApiService apiService)
        {
            _apiService = apiService;
            _bitwardenFileUploadService = new BitwardenFileUploadService(apiService);
            _azureFileUploadService = new AzureFileUploadService();
        }

        private readonly BitwardenFileUploadService _bitwardenFileUploadService;
        private readonly AzureFileUploadService _azureFileUploadService;
        private readonly ApiService _apiService;

        public async Task UploadSendFileAsync(SendFileUploadDataResponse uploadData, CipherString fileName, byte[] encryptedFileData)
        {
            try
            {
                switch (uploadData.FileUploadType)
                {
                    case FileUploadType.Direct:
                        await _bitwardenFileUploadService.Upload(fileName.EncryptedString, encryptedFileData,
                            fd => _apiService.PostSendFileAsync(uploadData.SendResponse.Id, uploadData.SendResponse.File.Id, fd));
                        break;
                    case FileUploadType.Azure:
                        Func<Task<string>> renewalCallback = async () =>
                        {
                            var response = await _apiService.RenewFileUploadUrlAsync(uploadData.SendResponse.Id, uploadData.SendResponse.File.Id);
                            return response.Url;
                        };
                        await _azureFileUploadService.Upload(uploadData.Url, encryptedFileData, renewalCallback);
                        break;
                    default:
                        throw new Exception("Unknown file upload type");
                }
            }
            catch (Exception e)
            {
                await _apiService.DeleteSendAsync(uploadData.SendResponse.Id);
                throw e;
            }
        }
    }
}
