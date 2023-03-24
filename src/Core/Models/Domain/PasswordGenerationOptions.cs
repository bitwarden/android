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
                AllowAmbiguousChar = true;
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
        public bool? AllowAmbiguousChar { get; set; }
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
            AllowAmbiguousChar = AllowAmbiguousChar ?? defaults.AllowAmbiguousChar;
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

        public void ApplyPolicies(PasswordGeneratorPolicyOptions enforcedPolicyOptions)
        {
            if (Length < enforcedPolicyOptions.MinLength)
            {
                Length = enforcedPolicyOptions.MinLength;
            }

            if (enforcedPolicyOptions.UseUppercase)
            {
                Uppercase = true;
            }

            if (enforcedPolicyOptions.UseLowercase)
            {
                Lowercase = true;
            }

            if (enforcedPolicyOptions.UseNumbers)
            {
                Number = true;
            }

            if (MinNumber < enforcedPolicyOptions.NumberCount)
            {
                MinNumber = enforcedPolicyOptions.NumberCount;
            }

            if (enforcedPolicyOptions.UseSpecial)
            {
                Special = true;
            }

            if (MinSpecial < enforcedPolicyOptions.SpecialCount)
            {
                MinSpecial = enforcedPolicyOptions.SpecialCount;
            }

            // Must normalize these fields because the receiving call expects all options to pass the current rules
            if (MinSpecial + MinNumber > Length)
            {
                MinSpecial = Length - MinNumber;
            }

            if (NumWords < enforcedPolicyOptions.MinNumberOfWords)
            {
                NumWords = enforcedPolicyOptions.MinNumberOfWords;
            }

            if (enforcedPolicyOptions.Capitalize)
            {
                Capitalize = true;
            }

            if (enforcedPolicyOptions.IncludeNumber)
            {
                IncludeNumber = true;
            }

            // Force default type if password/passphrase selected via policy
            if (enforcedPolicyOptions.DefaultType == "password" || enforcedPolicyOptions.DefaultType == "passphrase")
            {
                Type = enforcedPolicyOptions.DefaultType;
            }
        }
    }
}
