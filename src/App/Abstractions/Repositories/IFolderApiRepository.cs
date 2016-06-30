using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.App.Models.Api;

namespace Bit.App.Abstractions
{
    public interface IFolderApiRepository : IApiRepository<FolderRequest, FolderResponse, string>
    { }
}