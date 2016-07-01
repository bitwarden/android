using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.App.Models;
using Bit.App.Models.Api;

namespace Bit.App.Abstractions
{
    public interface IFolderService
    {
        Task<Folder> GetByIdAsync(string id);
        Task<IEnumerable<Folder>> GetAllAsync();
        Task<ApiResult<FolderResponse>> SaveAsync(Folder folder);
        Task<ApiResult> DeleteAsync(string folderId);
    }
}
