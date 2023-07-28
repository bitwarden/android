using Bit.Core.Models.Api;

namespace Bit.Core.Models.Data
{
    public class CardData : Data
    {
        public CardData() { }

        public CardData(CardApi data)
        {
            CardholderName = data.CardholderName;
            Brand = data.Brand;
            Number = data.Number;
            ExpMonth = data.ExpMonth;
            ExpYear = data.ExpYear;
            Code = data.Code;
        }

        public string CardholderName { get; set; }
        public string Brand { get; set; }
        public string Number { get; set; }
        public string ExpMonth { get; set; }
        public string ExpYear { get; set; }
        public string Code { get; set; }
    }
}
