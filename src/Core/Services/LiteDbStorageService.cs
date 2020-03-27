using Bit.Core.Abstractions;
using LiteDB;
using Newtonsoft.Json;
using Newtonsoft.Json.Serialization;
using System.Linq;
using System.Threading.Tasks;

namespace Bit.Core.Services
{
    public class LiteDbStorageService : IStorageService
    {
        private readonly JsonSerializerSettings _jsonSettings = new JsonSerializerSettings
        {
            ContractResolver = new CamelCasePropertyNamesContractResolver()
        };
        private readonly string _dbPath;
        private ILiteCollection<JsonItem> _collection;
        private Task _initTask;

        public LiteDbStorageService(string dbPath)
        {
            _dbPath = dbPath;
        }

        public Task InitAsync()
        {
            if (_collection != null)
            {
                return Task.FromResult(0);
            }
            if (_initTask != null)
            {
                return _initTask;
            }
            _initTask = Task.Run(() =>
            {
                try
                {
                    var db = new LiteDatabase($"Filename={_dbPath};Upgrade=true;");
                    _collection = db.GetCollection<JsonItem>("json_items");
                }
                finally
                {
                    _initTask = null;
                }
            });
            return _initTask;
        }

        public async Task<T> GetAsync<T>(string key)
        {
            await InitAsync();
            var item = _collection.Find(i => i.Id == key).FirstOrDefault();
            if (item == null)
            {
                return default(T);
            }
            return JsonConvert.DeserializeObject<T>(item.Value, _jsonSettings);
        }

        public async Task SaveAsync<T>(string key, T obj)
        {
            await InitAsync();
            var data = JsonConvert.SerializeObject(obj, _jsonSettings);
            _collection.Upsert(new JsonItem(key, data));
        }

        public async Task RemoveAsync(string key)
        {
            await InitAsync();
            _collection.DeleteMany(i => i.Id == key);
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
