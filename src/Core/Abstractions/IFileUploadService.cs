using System.Threading.Tasks;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Response;

namespace Bit.Core.Abstractions {
    public interface IFileUploadService {
        Task UploadCipherAttachmentFileAsync(AttachmentUploadDataResponse uploadData, EncString fileName, EncByteArray encryptedFileData);
        Task UploadSendFileAsync(SendFileUploadDataResponse uploadData, EncString fileName, EncByteArray encryptedFileData);
    }
}
