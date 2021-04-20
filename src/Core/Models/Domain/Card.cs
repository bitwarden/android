using Bit.Core.Models.Data;
using Bit.Core.Models.View;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace Bit.Core.Models.Domain
{
    public class Card : Domain
    {
        private HashSet<string> _map = new HashSet<string>
        {
            "CardholderName",
            "Brand",
            "Number",
            "ExpMonth",
            "ExpYear",
            "Code"
        };

        public Card() { }

        public Card(CardData obj, bool alreadyEncrypted = false)
        {
            BuildDomainModel(this, obj, _map, alreadyEncrypted);
        }

        public EncString CardholderName { get; set; }
        public EncString Brand { get; set; }
        public EncString Number { get; set; }
        public EncString ExpMonth { get; set; }
        public EncString ExpYear { get; set; }
        public EncString Code { get; set; }

        public Task<CardView> DecryptAsync(string orgId)
        {
            return DecryptObjAsync(new CardView(this), this, _map, orgId);
        }

        public CardData ToCardData()
        {
            var c = new CardData();
            BuildDataModel(this, c, _map);
            return c;
        }
    }
}
