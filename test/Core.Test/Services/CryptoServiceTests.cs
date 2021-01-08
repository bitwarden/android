
using System;
using System.Threading.Tasks;
using Xunit;
using Bit.Core.Abstractions;
using Bit.Core.Services;
using Bit.Core.Enums;
using Bit.Test.Common.AutoFixture;
using Bit.Test.Common.AutoFixture.Attributes;
using System.Text;

namespace Bit.Core.Test.Services
{
    public class CryptoServiceTests
    {
        const string regular256Key = "qBUmEYtwTwwGPuw/z6bs/qYXXYNUlocFlyAuuANI8Pw=";
        const string utf8256Key = "6DfJwW1R3txgiZKkIFTvVAb7qVlG7lKcmJGJoxR2GBU=";
        const string unicode256Key = "gejGI82xthA+nKtKmIh82kjw+ttHr+ODsUoGdu5sf0A=";
        const string regular512Key = "xe5cIG6ZfwGmb1FvsOedM0XKOm21myZkjL/eDeKIqqM=";
        const string utf8512Key = "XQMVBnxVEhlvjSFDQc77j5GDE9aorvbS0vKnjhRg0LY=";
        const string unicode512Key = "148GImrTbrjaGAe/iWEpclINM8Ehhko+9lB14+52lqc=";
        const string regularSalt = "salt";
        const string utf8Salt = "üser_salt";
        const string unicodeSalt = "😀salt🙏";
        const string regularInfo = "info";
        const string utf8Info = "üser_info";
        const string unicodeInfo = "😀info🙏";

        const string prk16Byte = "criAmKtfzxanbgea5/kelQ==";
        const string prk32Byte = "F5h4KdYQnIVH4rKH0P9CZb1GrR4n16/sJrS0PsQEn0Y=";
        const string prk64Byte = "ssBK0mRG17VHdtsgt8yo4v25CRNpauH+0r2fwY/E9rLyaFBAOMbIeTry+" +
            "gUJ28p8y+hFh3EI9pcrEWaNvFYonQ==";


        [Theory, SutAutoData]
        async public Task HkdfExpand_PrkTooSmall_Throws(SutProvider<CryptoService> sutProvider)
        {
            var exception = await Assert.ThrowsAsync<ArgumentException>(
                () => sutProvider.Sut.HkdfExpandAsync(Convert.FromBase64String(prk16Byte), "info", 32, HkdfAlgorithm.Sha256));
            Assert.Contains("too small", exception.Message);
        }

        [Theory, SutAutoData]
        async public Task HkdfoExpand_OutputTooBig_Throws(SutProvider<CryptoService> sutProvider)
        {
            var exception = await Assert.ThrowsAsync<ArgumentException>(
                () => sutProvider.Sut.HkdfExpandAsync(Convert.FromBase64String(prk32Byte), "info", 8161, HkdfAlgorithm.Sha256));
            Assert.Contains("too large", exception.Message);
        }

        [Theory]
        [InlineAutoSubData(regular256Key, HkdfAlgorithm.Sha256, prk16Byte, regularSalt, regularInfo)]
        [InlineAutoSubData(utf8256Key, HkdfAlgorithm.Sha256, prk16Byte, utf8Salt, utf8Info)]
        [InlineAutoSubData(unicode256Key, HkdfAlgorithm.Sha256, prk16Byte, unicodeSalt, unicodeInfo)]
        [InlineAutoSubData(regular512Key, HkdfAlgorithm.Sha512, prk16Byte, regularSalt, regularInfo)]
        [InlineAutoSubData(utf8512Key, HkdfAlgorithm.Sha512, prk16Byte, utf8Salt, utf8Info)]
        [InlineAutoSubData(unicode512Key, HkdfAlgorithm.Sha512, prk16Byte, unicodeSalt, unicodeInfo)]
        async public Task Hkdf_Success(string expectedKey, HkdfAlgorithm algorithm, string ikmString, string salt, string info, PclCryptoFunctionService cryptoFunctionService)
        {
            byte[] ikm = Convert.FromBase64String(ikmString);
            var sutProvider = new SutProvider<CryptoService>().SetDependency<ICryptoFunctionService>(cryptoFunctionService).Create();

            var key = await sutProvider.Sut.HkdfAsync(ikm, salt, info, 32, algorithm);
            Assert.Equal(expectedKey, Convert.ToBase64String(key));

            var keyFromByteArray = await sutProvider.Sut.HkdfAsync(ikm, Encoding.UTF8.GetBytes(salt), Encoding.UTF8.GetBytes(info), 32, algorithm);
            Assert.Equal(key, keyFromByteArray);
        }

        [Theory]
        [InlineAutoSubData("BnIqJlfnHm0e/2iB/15cbHyR19ARPIcWRp4oNS22CD8=",
                HkdfAlgorithm.Sha256, prk32Byte, 32, regularInfo)]
        [InlineAutoSubData("BnIqJlfnHm0e/2iB/15cbHyR19ARPIcWRp4oNS22CD9BV+/queOZenPNkDhmlVyL2WZ3OSU5+7ISNF5NhNfvZA==",
                HkdfAlgorithm.Sha256, prk32Byte, 64, regularInfo)]
        [InlineAutoSubData("uLWbMWodSBms5uGJ5WTRTesyW+MD7nlpCZvagvIRXlk=",
                HkdfAlgorithm.Sha512, prk64Byte, 32, regularInfo)]
        [InlineAutoSubData("uLWbMWodSBms5uGJ5WTRTesyW+MD7nlpCZvagvIRXlkY5Pv0sB+MqvaopmkC6sD/j89zDwTV9Ib2fpucUydO8w==",
                HkdfAlgorithm.Sha512, prk64Byte, 64, regularInfo)]
        async public Task HkdfExpand_Success(string expectedKey, HkdfAlgorithm algorithm, string prkString, int outputByteSize, string info, PclCryptoFunctionService cryptoFunctionService)
        {
            var prk = Convert.FromBase64String(prkString);
            var sutProvider = new SutProvider<CryptoService>().SetDependency<ICryptoFunctionService>(cryptoFunctionService).Create();

            var key = await sutProvider.Sut.HkdfExpandAsync(prk, info, outputByteSize, algorithm);
            Assert.Equal(expectedKey, Convert.ToBase64String(key));

            var keyFromByteArray = await sutProvider.Sut.HkdfExpandAsync(prk, Encoding.UTF8.GetBytes(info), outputByteSize, algorithm);
            Assert.Equal(key, keyFromByteArray);
        }
    }
}
