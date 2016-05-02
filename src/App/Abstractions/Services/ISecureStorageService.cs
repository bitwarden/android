namespace Bit.App.Abstractions
{
    public interface ISecureStorageService
    {
        void Store(string key, byte[] dataBytes);
        byte[] Retrieve(string key);
        void Delete(string key);
        bool Contains(string key);
    }
}
