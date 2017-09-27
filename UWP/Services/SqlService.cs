using Bit.App.Abstractions;
using SQLite;
using System;
using System.IO;
using Windows.ApplicationModel;
using Windows.Storage;

namespace Bit.UWP.Services
{
    public class SqlService : ISqlService
    {
        public SQLiteConnection GetConnection()
        {
            throw new NotImplementedException();
        }
    }
}