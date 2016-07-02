using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Bit.App.Abstractions;
using Plugin.Settings.Abstractions;

namespace Bit.App.Services
{
    public class PasswordGenerationService : IPasswordGenerationService
    {
        private readonly ISettings _settings;
        private Random _random = new Random();

        public PasswordGenerationService(ISettings settings)
        {
            _settings = settings;
        }

        public string GeneratePassword()
        {
            int minUppercase = 1,
                minLowercase = 1,
                minNumbers = _settings.GetValueOrDefault(Constants.PasswordGeneratorMinNumbers, 1),
                minSpecial = _settings.GetValueOrDefault(Constants.PasswordGeneratorMinSpecial, 1),
                length = _settings.GetValueOrDefault(Constants.PasswordGeneratorLength, 10);

            bool uppercase = _settings.GetValueOrDefault(Constants.PasswordGeneratorUppercase, true),
                lowercase = _settings.GetValueOrDefault(Constants.PasswordGeneratorLowercase, true),
                numbers = _settings.GetValueOrDefault(Constants.PasswordGeneratorNumbers, true),
                special = _settings.GetValueOrDefault(Constants.PasswordGeneratorSpecial, true),
                ambiguous = _settings.GetValueOrDefault(Constants.PasswordGeneratorAmbiguous, false);

            // Sanitize
            if(uppercase && minUppercase < 0)
            {
                minUppercase = 1;
            }
            if(lowercase && minLowercase < 0)
            {
                minLowercase = 1;
            }
            if(numbers && minNumbers < 0)
            {
                minNumbers = 1;
            }
            if(special && minSpecial < 0)
            {
                minSpecial = 1;
            }

            if(length < 1)
            {
                length = 10;
            }
            var minLength = minUppercase + minLowercase + minNumbers + minSpecial;
            if(length < minLength)
            {
                length = minLength;
            }

            var positionsBuilder = new StringBuilder();
            if(lowercase && minLowercase > 0)
            {
                for(int i = 0; i < minLowercase; i++)
                {
                    positionsBuilder.Append("l");
                }
            }
            if(uppercase && minUppercase > 0)
            {
                for(int i = 0; i < minUppercase; i++)
                {
                    positionsBuilder.Append("u");
                }
            }
            if(numbers && minNumbers > 0)
            {
                for(int i = 0; i < minNumbers; i++)
                {
                    positionsBuilder.Append("n");
                }
            }
            if(special && minSpecial > 0)
            {
                for(int i = 0; i < minSpecial; i++)
                {
                    positionsBuilder.Append("s");
                }
            }
            while(positionsBuilder.Length < length)
            {
                positionsBuilder.Append("a");
            }

            // Shuffle
            var positions = positionsBuilder.ToString().ToCharArray().OrderBy(a => _random.Next()).ToArray();

            // Build out other character sets
            var allCharSet = string.Empty;

            var lowercaseCharSet = "abcdefghijkmnopqrstuvwxyz";
            if(ambiguous)
            {
                lowercaseCharSet = string.Concat(lowercaseCharSet, "l");
            }
            if(lowercase)
            {
                allCharSet = string.Concat(allCharSet, lowercaseCharSet);
            }

            var uppercaseCharSet = "ABCDEFGHIJKLMNPQRSTUVWXYZ";
            if(ambiguous)
            {
                uppercaseCharSet = string.Concat(uppercaseCharSet, "O");
            }
            if(uppercase)
            {
                allCharSet = string.Concat(allCharSet, uppercaseCharSet);
            }

            var numberCharSet = "23456789";
            if(ambiguous)
            {
                numberCharSet = string.Concat(numberCharSet, "01");
            }
            if(numbers)
            {
                allCharSet = string.Concat(allCharSet, numberCharSet);
            }

            var specialCharSet = "!@#$%^&*";
            if(special)
            {
                allCharSet = string.Concat(allCharSet, specialCharSet);
            }

            var password = new StringBuilder();
            for(var i = 0; i < length; i++)
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

                var randomCharIndex = _random.Next(0, positionChars.Length - 1);
                password.Append(positionChars[randomCharIndex]);
            }

            return password.ToString();
        }
    }
}
