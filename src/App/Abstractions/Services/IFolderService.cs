using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.App.Models;

namespace Bit.App.Abstractions
{
    public interface IFolderService
    {
        Task<IEnumerable<Folder>> GetAllAsync();
        Task SaveAsync(Folder folder);
    }
}
