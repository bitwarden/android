using Bit.Core.Enums;

namespace Bit.Droid.Autofill
{
    public class SavedItem
    {
        public CipherType Type { get; set; }
        public LoginItem Login { get; set; }
        public CardItem Card { get; set; }

        public class LoginItem
        {
            public string Username { get; set; }
            public string Password { get; set; }
        }

        public class CardItem
        {
            public string Name { get; set; }
            public string Number { get; set; }
            public string ExpMonth { get; set; }
            public string ExpYear { get; set; }
            public string Code { get; set; }
        }
    }
}