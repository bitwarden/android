using System;
using System.IO;
using Bit.App.Abstractions;
using Foundation;
using SQLite;

namespace Bit.iOS.Core.Services
{
    public class SqlService : ISqlService
    {
        private SQLiteConnection _connection;

        public SQLiteConnection GetConnection()
        {
            if(_connection != null)
            {
                return _connection;
            }

            var sqliteFilename = "bitwarden.db3";
            var fileManager = new NSFileManager();
            var appGroupContainer = fileManager.GetContainerUrl("group.com.8bit.bitwarden");
            var libraryPath = Path.Combine(appGroupContainer.Path, "Library"); // Library folder
            var path = Path.Combine(libraryPath, sqliteFilename);
            Console.WriteLine(path);

            _connection = new SQLiteConnection(path,
               SQLiteOpenFlags.ReadWrite | SQLiteOpenFlags.Create | SQLiteOpenFlags.FullMutex | SQLiteOpenFlags.SharedCache);
            return _connection;
        }
    }
}
