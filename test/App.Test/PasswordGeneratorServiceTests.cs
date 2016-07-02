using System;
using System.Linq;
using Bit.App.Services;
using NSubstitute;
using Plugin.Settings.Abstractions;
using Xunit;

namespace Bit.App.Test
{
    public class PasswordGeneratorServiceTests
    {
        [Fact]
        public void GeneratesCorrectLength()
        {
            var settings = SetupDefaultSettings();
            settings.GetValueOrDefault(Constants.PasswordGeneratorLength, Arg.Any<int>()).Returns(25);

            var service = new PasswordGenerationService(settings);
            int i = 0;
            while(i < 100)
            {
                var password = service.GeneratePassword();
                Assert.True(password.Length == 25);
                i++;
            }
        }

        [Fact]
        public void GeneratesCorrectMinNumbers()
        {
            var settings = SetupDefaultSettings();
            settings.GetValueOrDefault(Constants.PasswordGeneratorMinNumbers, Arg.Any<int>()).Returns(25);

            var service = new PasswordGenerationService(settings);
            int i = 0;
            while(i < 100)
            {
                var password = service.GeneratePassword();
                Assert.True(password.Count(char.IsDigit) >= 25);
                i++;
            }
        }

        [Fact]
        public void GeneratesCorrectMinSpecial()
        {
            var settings = SetupDefaultSettings();
            settings.GetValueOrDefault(Constants.PasswordGeneratorMinSpecial, Arg.Any<int>()).Returns(25);

            var service = new PasswordGenerationService(settings);
            int i = 0;
            while(i < 100)
            {
                var password = service.GeneratePassword();
                Assert.True(password.Count(c => !char.IsLetterOrDigit(c)) >= 25);
                i++;
            }
        }

        [Fact]
        public void GeneratesCorrectNoUppercase()
        {
            var settings = SetupDefaultSettings();
            settings.GetValueOrDefault(Constants.PasswordGeneratorUppercase, Arg.Any<bool>()).Returns(false);

            var service = new PasswordGenerationService(settings);
            int i = 0;
            while(i < 100)
            {
                var password = service.GeneratePassword();
                Assert.True(password.Count(char.IsUpper) == 0);
                i++;
            }
        }

        [Fact]
        public void GeneratesCorrectUppercase()
        {
            var settings = SetupDefaultSettings();
            settings.GetValueOrDefault(Constants.PasswordGeneratorUppercase, Arg.Any<bool>()).Returns(true);
            settings.GetValueOrDefault(Constants.PasswordGeneratorLowercase, Arg.Any<bool>()).Returns(false);
            settings.GetValueOrDefault(Constants.PasswordGeneratorNumbers, Arg.Any<bool>()).Returns(false);
            settings.GetValueOrDefault(Constants.PasswordGeneratorSpecial, Arg.Any<bool>()).Returns(false);

            var service = new PasswordGenerationService(settings);
            int i = 0;
            while(i < 100)
            {
                var password = service.GeneratePassword();
                Assert.True(password.Count(char.IsUpper) == 50);
                i++;
            }
        }

        [Fact]
        public void GeneratesCorrectNoLowercase()
        {
            var settings = SetupDefaultSettings();
            settings.GetValueOrDefault(Constants.PasswordGeneratorLowercase, Arg.Any<bool>()).Returns(false);

            var service = new PasswordGenerationService(settings);
            int i = 0;
            while(i < 100)
            {
                var password = service.GeneratePassword();
                Assert.True(password.Count(char.IsLower) == 0);
                i++;
            }
        }

        [Fact]
        public void GeneratesCorrectLowercase()
        {
            var settings = SetupDefaultSettings();
            settings.GetValueOrDefault(Constants.PasswordGeneratorUppercase, Arg.Any<bool>()).Returns(false);
            settings.GetValueOrDefault(Constants.PasswordGeneratorLowercase, Arg.Any<bool>()).Returns(true);
            settings.GetValueOrDefault(Constants.PasswordGeneratorNumbers, Arg.Any<bool>()).Returns(false);
            settings.GetValueOrDefault(Constants.PasswordGeneratorSpecial, Arg.Any<bool>()).Returns(false);

            var service = new PasswordGenerationService(settings);
            int i = 0;
            while(i < 100)
            {
                var password = service.GeneratePassword();
                Assert.True(password.Count(char.IsLower) == 50);
                i++;
            }
        }

        [Fact]
        public void GeneratesCorrectMixed()
        {
            var settings = SetupDefaultSettings();
            var service = new PasswordGenerationService(settings);
            int i = 0;
            while(i < 100)
            {
                var password = service.GeneratePassword();
                Assert.True(password.Count(char.IsLower) >= 1);
                Assert.True(password.Count(char.IsDigit) >= 1);
                Assert.True(password.Count(char.IsUpper) >= 1);
                Assert.True(password.Count(c => !char.IsLetterOrDigit(c)) >= 1);
                i++;
            }
        }

        [Fact]
        public void GeneratesCorrectNoAmbiguous()
        {
            var settings = SetupDefaultSettings();
            settings.GetValueOrDefault(Constants.PasswordGeneratorAmbiguous, Arg.Any<bool>()).Returns(false);

            var service = new PasswordGenerationService(settings);
            int i = 0;
            while(i < 100)
            {
                var password = service.GeneratePassword();
                Assert.True(!password.Any(c => c == '1' || c == 'l' || c == '0' || c == 'O'));
                i++;
            }
        }

        private ISettings SetupDefaultSettings()
        {
            var settings = Substitute.For<ISettings>();
            settings.GetValueOrDefault(Constants.PasswordGeneratorLength, Arg.Any<int>()).Returns(50);
            settings.GetValueOrDefault(Constants.PasswordGeneratorMinNumbers, Arg.Any<int>()).Returns(1);
            settings.GetValueOrDefault(Constants.PasswordGeneratorMinSpecial, Arg.Any<int>()).Returns(1);
            settings.GetValueOrDefault(Constants.PasswordGeneratorUppercase, Arg.Any<bool>()).Returns(true);
            settings.GetValueOrDefault(Constants.PasswordGeneratorLowercase, Arg.Any<bool>()).Returns(true);
            settings.GetValueOrDefault(Constants.PasswordGeneratorSpecial, Arg.Any<bool>()).Returns(true);
            settings.GetValueOrDefault(Constants.PasswordGeneratorNumbers, Arg.Any<bool>()).Returns(true);
            settings.GetValueOrDefault(Constants.PasswordGeneratorAmbiguous, Arg.Any<bool>()).Returns(false);
            return settings;
        }
    }
}
