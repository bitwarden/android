using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using SQLite;

namespace Bit.App.Services
{
    public abstract class Repository<T, TId>
        where TId : IEquatable<TId> 
        where T : class, IDataObject<TId>, new()
    {
        protected readonly SQLiteConnection _connection;

        public Repository(ISqlService sqlite)
        {
            _connection = sqlite.GetConnection();
        }

        protected virtual Task<T> GetByIdAsync(TId id)
        {
            return Task.FromResult(_connection.Get<T>(id));
        }

        protected virtual Task<IEnumerable<T>> GetAllAsync()
        {
            return Task.FromResult(_connection.Table<T>().Cast<T>());
        }

        protected virtual Task CreateAsync(T obj)
        {
            _connection.Insert(obj);
            return Task.FromResult(0);
        }

        protected virtual Task ReplaceAsync(T obj)
        {
            _connection.Update(obj);
            return Task.FromResult(0);
        }

        protected virtual Task DeleteAsync(T obj)
        {
            _connection.Delete<T>(obj.Id);
            return Task.FromResult(0);
        }
    }
}
