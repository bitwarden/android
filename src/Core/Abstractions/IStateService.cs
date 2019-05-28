using System.Threading.Tasks;

namespace Bit.Core.Abstractions
{
    public interface IStateService
    {
        Task<T> GetAsync<T>(string key);
        Task RemoveAsync(string key);
        Task SaveAsync<T>(string key, T obj);
        Task PurgeAsync();
    }
}