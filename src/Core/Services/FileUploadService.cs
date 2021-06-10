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

        public async Task UploadCipherAttachmentFileAsync(AttachmentUploadDataResponse uploadData,
            EncString encryptedFileName, EncByteArray encryptedFileData)
        {
            try
            {
                switch (uploadData.FileUploadType)
                {
                    case FileUploadType.Direct:
                        await _bitwardenFileUploadService.Upload(encryptedFileName.EncryptedString, encryptedFileData,
                            fd => _apiService.PostAttachmentFileAsync(uploadData.CipherResponse.Id, uploadData.AttachmentId, fd));
                        break;
                    case FileUploadType.Azure:
                        Func<Task<string>> renewalCallback = async () =>
                        {
                            var response = await _apiService.RenewAttachmentUploadUrlAsync(uploadData.CipherResponse.Id, uploadData.AttachmentId);
                            return response.Url;
                        };
                        await _azureFileUploadService.Upload(uploadData.Url, encryptedFileData, renewalCallback);
                        break;
                    default:
                        throw new Exception($"Unkown file upload type: {uploadData.FileUploadType}");
                }
            } catch
            {
                await _apiService.DeleteCipherAttachmentAsync(uploadData.CipherResponse.Id, uploadData.AttachmentId);
                throw;
            }
        }

        public async Task UploadSendFileAsync(SendFileUploadDataResponse uploadData, EncString fileName, EncByteArray encryptedFileData)
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
            catch (Exception)
            {
                await _apiService.DeleteSendAsync(uploadData.SendResponse.Id);
                throw;
            }
        }
    }
}
