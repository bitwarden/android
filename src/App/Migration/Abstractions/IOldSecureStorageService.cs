namespace Bit.App.Migration.Abstractions
{
    public interface IOldSecureStorageService
    {
        bool Contains(string key);
        void Delete(string key);
        byte[] Retrieve(string key);
        void Store(string key, byte[] dataBytes);
    }
}
