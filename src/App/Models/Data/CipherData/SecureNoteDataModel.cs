using Bit.App.Models.Api;
using System;

namespace Bit.App.Models.Data
{
    public class SecureNoteDataModel : CipherDataModel
    {
        public SecureNoteDataModel() { }

        public SecureNoteDataModel(CipherResponse response)
            : base(response)
        {
            if(response?.SecureNote == null)
            {
                throw new ArgumentNullException(nameof(response.SecureNote));
            }

            Type = response.SecureNote.Type;
        }

        public Enums.SecureNoteType Type { get; set; }
    }
}
