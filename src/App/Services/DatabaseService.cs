using System;
using Bit.App.Abstractions;
using Bit.App.Models.Data;
using SQLite;

namespace Bit.App.Services
{
    public class DatabaseService : IDatabaseService
    {
        protected readonly SQLiteConnection _connection;

        public DatabaseService(ISqlService sqlService)
        {
            _connection = sqlService.GetConnection();
        }

        public void CreateTables()
        {
            _connection.CreateTable<FolderData>();
            _connection.CreateTable<LoginData>();
            _connection.CreateTable<AttachmentData>();
            _connection.CreateTable<SettingsData>();
        }
    }
}
