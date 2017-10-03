using Bit.App.Abstractions;
using SQLite;
using System.IO;
using Windows.Storage;

namespace Bit.UWP.Services
{
    public class SqlService : ISqlService
    {
        public SQLiteConnection GetConnection()
        {
            var sqliteFilename = "bitwarden.db3";
            var path = Path.Combine(ApplicationData.Current.LocalFolder.Path, sqliteFilename);
            var conn = new SQLiteConnection(path,
                SQLiteOpenFlags.ReadWrite | SQLiteOpenFlags.Create |
                SQLiteOpenFlags.FullMutex | SQLiteOpenFlags.SharedCache);

            // Return the database connection 
            return conn;
        }
    }
}