using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.App.Models;
using System;

namespace Bit.App.Abstractions
{
    public interface ICollectionService
    {
        Task<Collection> GetByIdAsync(string id);
        Task<IEnumerable<Collection>> GetAllAsync();
        Task<IEnumerable<Tuple<string, string>>> GetAllCipherAssociationsAsync();
    }
}
