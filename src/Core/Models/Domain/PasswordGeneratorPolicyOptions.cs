namespace Bit.Core.Models.Domain
{
    public class PasswordGeneratorPolicyOptions
    {
        public string DefaultType { get; set; } = string.Empty;
        public int MinLength { get; set; }
        public bool UseUppercase { get; set; }
        public bool UseLowercase { get; set; }
        public bool UseNumbers { get; set; }
        public int NumberCount { get; set; }
        public bool UseSpecial { get; set; }
        public int SpecialCount { get; set; }
        public int MinNumberOfWords { get; set; }
        public bool Capitalize { get; set; }
        public bool IncludeNumber { get; set; }

        public bool InEffect()
        {
            return DefaultType != string.Empty ||
                MinLength > 0 ||
                NumberCount > 0 ||
                SpecialCount > 0 ||
                UseUppercase ||
                UseLowercase ||
                UseNumbers ||
                UseSpecial ||
                MinNumberOfWords > 0 ||
                Capitalize ||
                IncludeNumber;
        }
    }
}
