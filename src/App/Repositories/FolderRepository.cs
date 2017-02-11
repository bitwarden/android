using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models.Data;

namespace Bit.App.Repositories
{
    public class FolderRepository : Repository<FolderData, string>, IFolderRepository
    {
        public FolderRepository(ISqlService sqlService)
            : base(sqlService)
        { }

        public Task<IEnumerable<FolderData>> GetAllByUserIdAsync(string userId)
        {
            var folders = Connection.Table<FolderData>().Where(f => f.UserId == userId).Cast<FolderData>();
            return Task.FromResult(folders);
        }

        public override Task DeleteAsync(string id)
        {
            var now = DateTime.UtcNow;
            DeleteWithLoginUpdateAsync(id, now);
            return Task.FromResult(0);
        }

        public Task DeleteWithLoginUpdateAsync(string id, DateTime revisionDate)
        {
            Connection.RunInTransaction(() =>
            {
                Connection.Execute("UPDATE Site SET FolderId = ?, RevisionDateTime = ? WHERE FolderId = ?", null, revisionDate, id);
                Connection.Delete<FolderData>(id);
            });

            return Task.FromResult(0);
        }
    }
}
