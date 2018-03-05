namespace Bit.App.Models.Api
{
    public class CardType
    {
        public CardType() { }

        public CardType(Cipher cipher)
        {
            CardholderName = cipher.Card.CardholderName?.EncryptedString;
            Brand = cipher.Card.Brand?.EncryptedString;
            Number = cipher.Card.Number?.EncryptedString;
            ExpMonth = cipher.Card.ExpMonth?.EncryptedString;
            ExpYear = cipher.Card.ExpYear?.EncryptedString;
            Code = cipher.Card.Code?.EncryptedString;
        }

        public string CardholderName { get; set; }
        public string Brand { get; set; }
        public string Number { get; set; }
        public string ExpMonth { get; set; }
        public string ExpYear { get; set; }
        public string Code { get; set; }
    }
}
