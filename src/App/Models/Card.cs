using Bit.App.Models.Api;
using Bit.App.Models.Data;
using Newtonsoft.Json;

namespace Bit.App.Models
{
    public class Card
    {
        public Card(CipherData data)
        {
            var deserializedData = JsonConvert.DeserializeObject<CardDataModel>(data.Data);

            CardholderName = deserializedData.CardholderName != null ?
                new CipherString(deserializedData.CardholderName) : null;
            Brand = deserializedData.Brand != null ? new CipherString(deserializedData.Brand) : null;
            Number = deserializedData.Number != null ? new CipherString(deserializedData.Number) : null;
            ExpMonth = deserializedData.ExpMonth != null ? new CipherString(deserializedData.ExpMonth) : null;
            ExpYear = deserializedData.ExpYear != null ? new CipherString(deserializedData.ExpYear) : null;
            Code = deserializedData.Code != null ? new CipherString(deserializedData.Code) : null;
        }

        public CipherString CardholderName { get; set; }
        public CipherString Brand { get; set; }
        public CipherString Number { get; set; }
        public CipherString ExpMonth { get; set; }
        public CipherString ExpYear { get; set; }
        public CipherString Code { get; set; }
    }
}
