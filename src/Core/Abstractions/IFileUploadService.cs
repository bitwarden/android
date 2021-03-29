using System.Threading.Tasks;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Response;

namespace Bit.Core.Abstractions {
    public interface IFileUploadService {
        Task UploadSendFileAsync(SendFileUploadDataResponse uploadData, CipherString fileName, byte[] encryptedFileData);
    }
}
