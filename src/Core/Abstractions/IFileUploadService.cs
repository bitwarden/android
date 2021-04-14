using System.Threading.Tasks;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Response;

namespace Bit.Core.Abstractions {
    public interface IFileUploadService {
        Task UploadCipherAttachmentFileAsync(AttachmentUploadDataResponse uploadData, string fileName, CipherByteArray encryptedFileData);
        Task UploadSendFileAsync(SendFileUploadDataResponse uploadData, CipherString fileName, CipherByteArray encryptedFileData);
    }
}
