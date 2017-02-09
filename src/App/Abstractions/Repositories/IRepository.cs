using System;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace Bit.App.Abstractions
{
    public interface IRepository<T, TId>
        where T : class, IDataObject<TId>, new()
        where TId : IEquatable<TId>
    {
        Task<T> GetByIdAsync(TId id);
        Task<IEnumerable<T>> GetAllAsync();
        Task UpdateAsync(T obj);
        Task InsertAsync(T obj);
        Task UpsertAsync(T obj);
        Task DeleteAsync(TId id);
        Task DeleteAsync(T obj);
    }
}
