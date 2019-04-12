using Bit.Core.Models.Domain;

namespace Bit.Core.Models.View
{
    public class CardView : View
    {
        public CardView() { }

        public CardView(Card c) { }

        public string Id { get; set; }
        public string Url { get; set; }
        public string Size { get; set; }
        public string SizeName { get; set; }
        public string FileName { get; set; }
        public SymmetricCryptoKey Key { get; set; }

        // TODO
    }
}
