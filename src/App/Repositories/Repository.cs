using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Bit.App.Abstractions;
using SQLite;

namespace Bit.App.Repositories
{
    public abstract class Repository<T, TId> : IRepository<T, TId>
        where TId : IEquatable<TId>
        where T : class, IDataObject<TId>, new()
    {
        public Repository(ISqlService sqlService)
        {
            Connection = sqlService.GetConnection();
        }

        protected SQLiteConnection Connection { get; private set; }

        public virtual Task<T> GetByIdAsync(TId id)
        {
            return Task.FromResult(Connection.Find<T>(id));
        }

        public virtual Task<IEnumerable<T>> GetAllAsync()
        {
            return Task.FromResult(Connection.Table<T>().Cast<T>());
        }

        public virtual Task InsertAsync(T obj)
        {
            Connection.Insert(obj);
            return Task.FromResult(0);
        }

        public virtual Task UpdateAsync(T obj)
        {
            Connection.Update(obj);
            return Task.FromResult(0);
        }
        public virtual Task UpsertAsync(T obj)
        {
            Connection.InsertOrReplace(obj);
            return Task.FromResult(0);
        }

        public virtual async Task DeleteAsync(T obj)
        {
            await DeleteAsync(obj.Id);
        }

        public virtual Task DeleteAsync(TId id)
        {
            Connection.Delete<T>(id);
            return Task.FromResult(0);
        }
    }
}
