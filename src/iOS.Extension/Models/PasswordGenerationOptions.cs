using System;

namespace Bit.iOS.Extension.Models
{
    public class PasswordGenerationOptions
    {
        public int MinLength { get; set; }
        public int MaxLength { get; set; }
        public bool RequireDigits { get; set; }
        public bool RequireSymbols { get; set; }
        public string ForbiddenCharacters { get; set; }
    }
}
