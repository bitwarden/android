using System;
using System.IO;
using Bit.App.Abstractions;
using SQLite;

namespace Bit.Android.Services
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
            var documentsPath = Environment.GetFolderPath(Environment.SpecialFolder.Personal); // Documents folder
            var path = Path.Combine(documentsPath, sqliteFilename);
            Console.WriteLine(path);

            _connection = new SQLiteConnection(path,
                SQLiteOpenFlags.ReadWrite | SQLiteOpenFlags.Create | SQLiteOpenFlags.FullMutex | SQLiteOpenFlags.SharedCache);
            return _connection;
        }
    }
}
