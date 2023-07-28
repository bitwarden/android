using System.Threading.Tasks;

namespace Bit.Core.Abstractions
{
    public interface IStorageService
    {
        Task<T> GetAsync<T>(string key);
        Task SaveAsync<T>(string key, T obj);
        Task RemoveAsync(string key);
    }
}
