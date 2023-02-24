namespace Bit.Core.Abstractions
{
    public interface ISynchronousStorageService
    {
        T Get<T>(string key);
        void Save<T>(string key, T obj);
        void Remove(string key);
    }
}
