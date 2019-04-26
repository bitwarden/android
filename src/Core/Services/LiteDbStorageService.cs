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
        private LiteCollection<JsonItem> _collection;

        public LiteDbStorageService(string dbPath)
        {
            _dbPath = dbPath;
        }

        public void Init()
        {
            if(_collection == null)
            {
                var db = new LiteDatabase($"Filename={_dbPath};");
                _collection = db.GetCollection<JsonItem>("json_items");
            }
        }

        public Task<T> GetAsync<T>(string key)
        {
            Init();
            var item = _collection.Find(i => i.Id == key).FirstOrDefault();
            if(item == null)
            {
                return Task.FromResult(default(T));
            }
            return Task.FromResult(JsonConvert.DeserializeObject<T>(item.Value, _jsonSettings));
        }

        public Task SaveAsync<T>(string key, T obj)
        {
            Init();
            var data = JsonConvert.SerializeObject(obj, _jsonSettings);
            _collection.Upsert(new JsonItem(key, data));
            return Task.FromResult(0);
        }

        public Task RemoveAsync(string key)
        {
            Init();
            _collection.Delete(i => i.Id == key);
            return Task.FromResult(0);
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
