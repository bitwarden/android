using System;
using System.IO;
using Bit.App.Abstractions;

namespace Bit.Android.Services
{
    public class SqlService : ISqlService
    {
        public SQLite.SQLiteConnection GetConnection()
        {
            var sqliteFilename = "bitwarden.db3";
            var documentsPath = Environment.GetFolderPath(Environment.SpecialFolder.Personal); // Documents folder
            var path = Path.Combine(documentsPath, sqliteFilename);

            Console.WriteLine(path);
            var conn = new SQLite.SQLiteConnection(path);

            // Return the database connection 
            return conn;
        }
    }
}
