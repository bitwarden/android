using System.Threading.Tasks;
using Bit.Core.Services;

namespace Bit.Core.Abstractions
{
    public interface IStorageMediatorService
    {
        T Get<T>(string key);
        void Save<T>(string key, T obj);
        void Remove(string key);

        Task<T> GetAsync<T>(string key, StorageMediatorOptions options = default);
        Task SaveAsync<T>(string key, T obj, StorageMediatorOptions options = default);
        Task RemoveAsync(string key, StorageMediatorOptions options = default);
    }
}
