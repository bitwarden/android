using System.Threading.Tasks;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Response;

namespace Bit.Core.Abstractions {
    public interface IFileUploadService {
        Task UploadCipherAttachmentFileAsync(AttachmentUploadDataResponse uploadData, string fileName, byte[] encryptedFileData);
        Task UploadSendFileAsync(SendFileUploadDataResponse uploadData, CipherString fileName, byte[] encryptedFileData);
    }
}
