namespace Bit.App.Models.Api
{
    public class CardDataModel : CipherDataModel
    {
        public string CardholderName { get; set; }
        public string Brand { get; set; }
        public string Number { get; set; }
        public string ExpMonth { get; set; }
        public string ExpYear { get; set; }
        public string Code { get; set; }
    }
}
