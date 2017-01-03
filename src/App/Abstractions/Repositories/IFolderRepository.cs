using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.App.Models.Data;

namespace Bit.App.Abstractions
{
    public interface IFolderRepository : IRepository<FolderData, string>
    {
        Task<IEnumerable<FolderData>> GetAllByUserIdAsync(string userId);
        Task DeleteWithLoginUpdateAsync(string id, DateTime revisionDate);
    }
}
