using System.Threading.Tasks;
using Bit.Core.Services;

namespace Bit.Core.Abstractions
{
    public interface IStorageMediatorService
    {
        T Get<T>(string key);
        void Save<T>(string key, T obj);
        void Remove(string key);

        Task<T> GetAsync<T>(string key, bool useSecureStorage = false);
        Task SaveAsync<T>(string key, T obj, bool useSecureStorage = false, bool allowSaveNull = false);
        Task RemoveAsync(string key, bool useSecureStorage = false);
    }
}
