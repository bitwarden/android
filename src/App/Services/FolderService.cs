using System;
using System.Linq;
using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using Bit.App.Models;
using Bit.App.Models.Data;

namespace Bit.App.Services
{
    public class FolderService : Repository<FolderData, int>, IFolderService
    {
        public FolderService(ISqlService sqlite)
            : base(sqlite) { }

        public new async Task<IEnumerable<Folder>> GetAllAsync()
        {
            var data = await base.GetAllAsync();
            return data.Select(f => new Folder(f));
        }

        public async Task SaveAsync(Folder folder)
        {
            var data = new FolderData(folder);
            data.RevisionDateTime = DateTime.UtcNow;

            if(folder.Id == 0)
            {
                await CreateAsync(data);
            }
            else
            {
                await ReplaceAsync(data);
            }

            folder.Id = data.Id;
        }
    }
}
