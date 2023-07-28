using System.Linq;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using LiteDB;
using Newtonsoft.Json;
using Newtonsoft.Json.Serialization;

namespace Bit.Core.Services
{
    public class LiteDbStorageService : IStorageService
    {
        private static readonly object _lock = new object();

        private readonly JsonSerializerSettings _jsonSettings = new JsonSerializerSettings
        {
            ContractResolver = new CamelCasePropertyNamesContractResolver()
        };
        private readonly string _dbPath;

        public LiteDbStorageService(string dbPath)
        {
            _dbPath = dbPath;
        }

        private LiteDatabase GetDb()
        {
            return new LiteDatabase($"Filename={_dbPath};Upgrade=true;");
        }

        private ILiteCollection<JsonItem> GetCollection(LiteDatabase db)
        {
            return db?.GetCollection<JsonItem>("json_items");
        }

        public Task<T> GetAsync<T>(string key)
        {
            lock (_lock)
            {
                LiteDatabase db = null;
                try
                {
                    db = GetDb();
                    var collection = GetCollection(db);
                    if (db == null || collection == null)
                    {
                        return Task.FromResult(default(T));
                    }
                    var item = collection.Find(i => i.Id == key).FirstOrDefault();
                    if (item == null)
                    {
                        return Task.FromResult(default(T));
                    }
                    return Task.FromResult(JsonConvert.DeserializeObject<T>(item.Value, _jsonSettings));
                }
                finally
                {
                    db?.Dispose();
                }
            }
        }

        public Task SaveAsync<T>(string key, T obj)
        {
            lock (_lock)
            {
                LiteDatabase db = null;
                try
                {
                    db = GetDb();
                    var collection = GetCollection(db);
                    if (db == null || collection == null)
                    {
                        return Task.CompletedTask;
                    }
                    var data = JsonConvert.SerializeObject(obj, _jsonSettings);
                    collection.Upsert(new JsonItem(key, data));
                    return Task.CompletedTask;
                }
                finally
                {
                    db?.Dispose();
                }
            }
        }

        public Task RemoveAsync(string key)
        {
            lock (_lock)
            {
                LiteDatabase db = null;
                try
                {
                    db = GetDb();
                    var collection = GetCollection(db);
                    if (db == null || collection == null)
                    {
                        return Task.CompletedTask;
                    }
                    collection.DeleteMany(i => i.Id == key);
                    return Task.CompletedTask;
                }
                finally
                {
                    db?.Dispose();
                }
            }
        }

        private class JsonItem
        {
            public JsonItem() { }

            public JsonItem(string key, string value)
            {
                Id = key;
                Value = value;
            }

            public string Id { get; set; }
            public string Value { get; set; }
        }
    }
}
