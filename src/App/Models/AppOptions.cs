using System;
using Bit.Core.Enums;

namespace Bit.App.Models
{
    public class AppOptions
    {
        public bool MyVaultTile { get; set; }
        public bool GeneratorTile { get; set; }
        public bool FromAutofillFramework { get; set; }
        public CipherType? FillType { get; set; }
        public string Uri { get; set; }
        public CipherType? SaveType { get; set; }
        public string SaveName { get; set; }
        public string SaveUsername { get; set; }
        public string SavePassword { get; set; }
        public string SaveCardName { get; set; }
        public string SaveCardNumber { get; set; }
        public string SaveCardExpMonth { get; set; }
        public string SaveCardExpYear { get; set; }
        public string SaveCardCode { get; set; }
        public bool IosExtension { get; set; }
        public Tuple<SendType, string, byte[], string> CreateSend { get; set; }

        public void SetAllFrom(AppOptions o)
        {
            if (o == null)
            {
                return;
            }
            MyVaultTile = o.MyVaultTile;
            GeneratorTile = o.GeneratorTile;
            FromAutofillFramework = o.FromAutofillFramework;
            FillType = o.FillType;
            Uri = o.Uri;
            SaveType = o.SaveType;
            SaveName = o.SaveName;
            SaveUsername = o.SaveUsername;
            SavePassword = o.SavePassword;
            SaveCardName = o.SaveCardName;
            SaveCardNumber = o.SaveCardNumber;
            SaveCardExpMonth = o.SaveCardExpMonth;
            SaveCardExpYear = o.SaveCardExpYear;
            SaveCardCode = o.SaveCardCode;
            IosExtension = o.IosExtension;
            CreateSend = o.CreateSend;
        }
    }
}
