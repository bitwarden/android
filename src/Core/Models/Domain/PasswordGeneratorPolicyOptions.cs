namespace Bit.Core.Models.Domain
{
    public class PasswordGeneratorPolicyOptions
    {
        public int MinLength { get; set; }
        public bool UseUppercase { get; set; }
        public bool UseLowercase { get; set; }
        public bool UseNumbers { get; set; }
        public int NumberCount { get; set; }
        public bool UseSpecial { get; set; }
        public int SpecialCount { get; set; }
    }
}
