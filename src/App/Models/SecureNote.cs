using Bit.App.Enums;
using Bit.App.Models.Data;
using Newtonsoft.Json;
using System;

namespace Bit.App.Models
{
    public class SecureNote
    {
        public SecureNote() { }

        public SecureNote(CipherData data)
        {
            SecureNoteDataModel deserializedData;
            if(data.SecureNote != null)
            {
                deserializedData = JsonConvert.DeserializeObject<SecureNoteDataModel>(data.SecureNote);
            }
            else if(data.Data != null)
            {
                deserializedData = JsonConvert.DeserializeObject<SecureNoteDataModel>(data.Data);
            }
            else
            {
                throw new ArgumentNullException(nameof(data.Identity));
            }

            Type = deserializedData.Type;
        }

        public SecureNoteType Type { get; set; }
    }
}
