using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Bit.App.Abstractions;
using Plugin.Settings.Abstractions;
using PCLCrypto;

namespace Bit.App.Services
{
    public class PasswordGenerationService : IPasswordGenerationService
    {
        private readonly ISettings _settings;

        public PasswordGenerationService(ISettings settings)
        {
            _settings = settings;
        }

        public string GeneratePassword(
            int? length = null,
            bool? uppercase = null,
            bool? lowercase = null,
            bool? numbers = null,
            bool? special = null,
            bool? ambiguous = null,
            int? minUppercase = null,
            int? minLowercase = null,
            int? minNumbers = null,
            int? minSpecial = null)
        {
            int minUppercaseValue = minUppercase.GetValueOrDefault(0),
                minLowercaseValue = minLowercase.GetValueOrDefault(0),
                minNumbersValue = minNumbers.GetValueOrDefault(_settings.GetValueOrDefault(Constants.PasswordGeneratorMinNumbers, 1)),
                minSpecialValue = minSpecial.GetValueOrDefault(_settings.GetValueOrDefault(Constants.PasswordGeneratorMinSpecial, 1)),
                lengthValue = length.GetValueOrDefault(_settings.GetValueOrDefault(Constants.PasswordGeneratorLength, 10));

            bool uppercaseValue = uppercase.GetValueOrDefault(_settings.GetValueOrDefault(Constants.PasswordGeneratorUppercase, true)),
                lowercaseValue = lowercase.GetValueOrDefault(_settings.GetValueOrDefault(Constants.PasswordGeneratorLowercase, true)),
                numbersValue = numbers.GetValueOrDefault(_settings.GetValueOrDefault(Constants.PasswordGeneratorNumbers, true)),
                specialValue = special.GetValueOrDefault(_settings.GetValueOrDefault(Constants.PasswordGeneratorSpecial, true)),
                ambiguousValue = ambiguous.GetValueOrDefault(_settings.GetValueOrDefault(Constants.PasswordGeneratorAmbiguous, false));

            // Sanitize
            if(uppercaseValue && minUppercaseValue <= 0)
            {
                minUppercaseValue = 1;
            }
            if(lowercaseValue && minLowercaseValue <= 0)
            {
                minLowercaseValue = 1;
            }
            if(numbersValue && minNumbersValue <= 0)
            {
                minNumbersValue = 1;
            }
            if(specialValue && minSpecialValue <= 0)
            {
                minSpecialValue = 1;
            }

            if(lengthValue < 1)
            {
                lengthValue = 10;
            }
            var minLength = minUppercaseValue + minLowercaseValue + minNumbersValue + minSpecialValue;
            if(lengthValue < minLength)
            {
                lengthValue = minLength;
            }

            var positionsBuilder = new StringBuilder();
            if(lowercaseValue && minLowercaseValue > 0)
            {
                for(int i = 0; i < minLowercaseValue; i++)
                {
                    positionsBuilder.Append("l");
                }
            }
            if(uppercaseValue && minUppercaseValue > 0)
            {
                for(int i = 0; i < minUppercaseValue; i++)
                {
                    positionsBuilder.Append("u");
                }
            }
            if(numbersValue && minNumbersValue > 0)
            {
                for(int i = 0; i < minNumbersValue; i++)
                {
                    positionsBuilder.Append("n");
                }
            }
            if(specialValue && minSpecialValue > 0)
            {
                for(int i = 0; i < minSpecialValue; i++)
                {
                    positionsBuilder.Append("s");
                }
            }
            while(positionsBuilder.Length < lengthValue)
            {
                positionsBuilder.Append("a");
            }

            // Shuffle
            var positions = positionsBuilder.ToString().ToCharArray().OrderBy(a => Next(int.MaxValue)).ToArray();

            // Build out other character sets
            var allCharSet = string.Empty;

            var lowercaseCharSet = "abcdefghijkmnopqrstuvwxyz";
            if(ambiguousValue)
            {
                lowercaseCharSet = string.Concat(lowercaseCharSet, "l");
            }
            if(lowercaseValue)
            {
                allCharSet = string.Concat(allCharSet, lowercaseCharSet);
            }

            var uppercaseCharSet = "ABCDEFGHIJKLMNPQRSTUVWXYZ";
            if(ambiguousValue)
            {
                uppercaseCharSet = string.Concat(uppercaseCharSet, "O");
            }
            if(uppercaseValue)
            {
                allCharSet = string.Concat(allCharSet, uppercaseCharSet);
            }

            var numberCharSet = "23456789";
            if(ambiguousValue)
            {
                numberCharSet = string.Concat(numberCharSet, "01");
            }
            if(numbersValue)
            {
                allCharSet = string.Concat(allCharSet, numberCharSet);
            }

            var specialCharSet = "!@#$%^&*";
            if(specialValue)
            {
                allCharSet = string.Concat(allCharSet, specialCharSet);
            }

            var password = new StringBuilder();
            for(var i = 0; i < lengthValue; i++)
            {
                string positionChars = string.Empty;
                switch(positions[i])
                {
                    case 'l':
                        positionChars = lowercaseCharSet;
                        break;
                    case 'u':
                        positionChars = uppercaseCharSet;
                        break;
                    case 'n':
                        positionChars = numberCharSet;
                        break;
                    case 's':
                        positionChars = specialCharSet;
                        break;
                    case 'a':
                        positionChars = allCharSet;
                        break;
                }

                var randomCharIndex = Next(positionChars.Length - 1);
                password.Append(positionChars[randomCharIndex]);
            }

            return password.ToString();
        }

        private int Next(int maxValue)
        {
            if(maxValue == 0)
            {
                return 0;
            }

            return (int)(WinRTCrypto.CryptographicBuffer.GenerateRandomNumber() % maxValue);
        }
    }
}
