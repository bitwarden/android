using Bit.App.Models.Data;
using Newtonsoft.Json;
using System;

namespace Bit.App.Models
{
    public class Card
    {
        public Card() { }

        public Card(CipherData data)
        {
            CardDataModel deserializedData;
            if(data.Card != null)
            {
                deserializedData = JsonConvert.DeserializeObject<CardDataModel>(data.Card);
            }
            else if(data.Data != null)
            {
                deserializedData = JsonConvert.DeserializeObject<CardDataModel>(data.Data);
            }
            else
            {
                throw new ArgumentNullException(nameof(data.Card));
            }

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
