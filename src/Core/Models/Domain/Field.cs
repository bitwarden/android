using System.Collections.Generic;
using System.Threading.Tasks;
using Bit.Core.Enums;
using Bit.Core.Models.Data;
using Bit.Core.Models.View;

namespace Bit.Core.Models.Domain
{
    public class Field : Domain
    {
        private HashSet<string> _map = new HashSet<string>
        {
            "Name",
            "Value"
        };

        public Field() { }

        public Field(FieldData obj, bool alreadyEncrypted = false)
        {
            Type = obj.Type;
            LinkedId = obj.LinkedId;
            BuildDomainModel(this, obj, _map, alreadyEncrypted);
        }

        public EncString Name { get; set; }
        public EncString Value { get; set; }
        public FieldType Type { get; set; }
        public LinkedIdType? LinkedId { get; set; }

        public Task<FieldView> DecryptAsync(string orgId, SymmetricCryptoKey key = null)
        {
            return DecryptObjAsync(new FieldView(this), this, _map, orgId, key);
        }

        public FieldData ToFieldData()
        {
            var f = new FieldData();
            BuildDataModel(this, f, new HashSet<string>
            {
                "Name",
                "Value",
                "Type",
                "LinkedId"
            }, new HashSet<string>
            {
                "Type",
                "LinkedId"
            });
            return f;
        }
    }
}
