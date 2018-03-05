using Bit.App.Models.Api;
using System;

namespace Bit.App.Models.Data
{
    public class CardDataModel : CipherDataModel
    {
        public CardDataModel() { }

        public CardDataModel(CipherResponse response)
            : base(response)
        {
            if(response?.Card == null)
            {
                throw new ArgumentNullException(nameof(response.Card));
            }

            CardholderName = response.Card.CardholderName;
            Brand = response.Card.Brand;
            Number = response.Card.Number;
            ExpMonth = response.Card.ExpMonth;
            ExpYear = response.Card.ExpYear;
            Code = response.Card.Code;
        }

        public string CardholderName { get; set; }
        public string Brand { get; set; }
        public string Number { get; set; }
        public string ExpMonth { get; set; }
        public string ExpYear { get; set; }
        public string Code { get; set; }
    }
}
