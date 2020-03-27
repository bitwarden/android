namespace Bit.Core.Models.Domain
{
    public class PasswordGenerationOptions
    {
        public PasswordGenerationOptions() { }

        public PasswordGenerationOptions(bool defaultOptions)
        {
            if (defaultOptions)
            {
                Length = 14;
                Ambiguous = false;
                Number = true;
                MinNumber = 1;
                Uppercase = true;
                MinUppercase = 0;
                Lowercase = true;
                MinLowercase = 0;
                Special = false;
                MinSpecial = 1;
                Type = "password";
                NumWords = 3;
                WordSeparator = "-";
                Capitalize = false;
                IncludeNumber = false;
            }
        }

        public int? Length { get; set; }
        public bool? Ambiguous { get; set; }
        public bool? Number { get; set; }
        public int? MinNumber { get; set; }
        public bool? Uppercase { get; set; }
        public int? MinUppercase { get; set; }
        public bool? Lowercase { get; set; }
        public int? MinLowercase { get; set; }
        public bool? Special { get; set; }
        public int? MinSpecial { get; set; }
        public string Type { get; set; }
        public int? NumWords { get; set; }
        public string WordSeparator { get; set; }
        public bool? Capitalize { get; set; }
        public bool? IncludeNumber { get; set; }

        public void Merge(PasswordGenerationOptions defaults)
        {
            Length = Length ?? defaults.Length;
            Ambiguous = Ambiguous ?? defaults.Ambiguous;
            Number = Number ?? defaults.Number;
            MinNumber = MinNumber ?? defaults.MinNumber;
            Uppercase = Uppercase ?? defaults.Uppercase;
            MinUppercase = MinUppercase ?? defaults.MinUppercase;
            Lowercase = Lowercase ?? defaults.Lowercase;
            MinLowercase = MinLowercase ?? defaults.MinLowercase;
            Special = Special ?? defaults.Special;
            MinSpecial = MinSpecial ?? defaults.MinSpecial;
            Type = Type ?? defaults.Type;
            NumWords = NumWords ?? defaults.NumWords;
            WordSeparator = WordSeparator ?? defaults.WordSeparator;
            Capitalize = Capitalize ?? defaults.Capitalize;
            IncludeNumber = IncludeNumber ?? defaults.IncludeNumber;
        }
    }
}
