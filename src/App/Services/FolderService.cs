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
        private readonly IAuthService _authService;

        public FolderService(
            ISqlService sqlService,
            IAuthService authService)
            : base(sqlService)
        {
            _authService = authService;
        }

        public new Task<IEnumerable<Folder>> GetAllAsync()
        {
            var data = Connection.Table<FolderData>().Where(f => f.UserId == _authService.UserId).Cast<FolderData>();
            return Task.FromResult(data.Select(f => new Folder(f)));
        }

        public async Task SaveAsync(Folder folder)
        {
            var data = new FolderData(folder, _authService.UserId);
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
