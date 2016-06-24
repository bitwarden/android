using System;
using System.IO;
using Bit.App.Abstractions;
using Foundation;

namespace Bit.iOS.Core.Services
{
    public class SqlService : ISqlService
    {
        public SQLite.SQLiteConnection GetConnection()
        {
            var sqliteFilename = "bitwarden.db3";
            var fileManager = new NSFileManager();
            var appGroupContainer = fileManager.GetContainerUrl("group.com.8bit.bitwarden");
            var libraryPath = Path.Combine(appGroupContainer.Path, "Library"); // Library folder
            var path = Path.Combine(libraryPath, sqliteFilename);

            Console.WriteLine(path);
            var conn = new SQLite.SQLiteConnection(path);

            // Return the database connection 
            return conn;
        }
    }
}
