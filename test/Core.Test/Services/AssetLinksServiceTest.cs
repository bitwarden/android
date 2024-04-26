using System;
using System.Collections.Generic;
using System.IO;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Resources.Localization;
using Bit.Core.Services;
using Bit.Core.Utilities.DigitalAssetLinks;
using Bit.Test.Common.AutoFixture;
using Newtonsoft.Json;
using NSubstitute;
using Xunit;

namespace Bit.Core.Test.Services
{
    public class AssetLinksServiceTest : IDisposable
    {
        private readonly SutProvider<AssetLinksService> _sutProvider = new SutProvider<AssetLinksService>().Create();

        private readonly string _validRpId = "example.com";
        private readonly string _validPackageName = "com.example.app";
        private readonly string _validFingerprint = "00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00";

        private List<Statement> Deserialize(string json)
        {
            return JsonConvert.DeserializeObject<List<Statement>>(json);
        }
        
        [Fact]
        public async Task ValidateAssetLinksAsync_Returns_True_When_Data_Has_One_Statement_And_One_Fingerprint()
        {
            // Arrange
            _sutProvider.GetDependency<IApiService>()
                .GetDigitalAssetLinksForRpAsync(_validRpId)
                .Returns(Task.FromResult(Deserialize(BasicAssetLinksTestData.OneStatementOneFingerprintJson())));

            // Act
            var isValid = await _sutProvider.Sut.ValidateAssetLinksAsync(_validRpId, _validPackageName, _validFingerprint);

            // Assert
            Assert.True(isValid);
        }
        
        [Fact]
        public async Task ValidateAssetLinksAsync_Returns_True_When_Data_Has_One_Statement_And_Multiple_Fingerprints()
        {
            // Arrange
            _sutProvider.GetDependency<IApiService>()
                .GetDigitalAssetLinksForRpAsync(_validRpId)
                .Returns(Task.FromResult(Deserialize(BasicAssetLinksTestData.OneStatementMultipleFingerprintsJson())));

            // Act
            var isValid = await _sutProvider.Sut.ValidateAssetLinksAsync(_validRpId, _validPackageName, _validFingerprint);

            // Assert
            Assert.True(isValid);
        }
        
        [Fact]
        public async Task ValidateAssetLinksAsync_Returns_True_When_Data_Has_Multiple_Statements()
        {
            // Arrange
            _sutProvider.GetDependency<IApiService>()
                .GetDigitalAssetLinksForRpAsync(_validRpId)
                .Returns(Task.FromResult(Deserialize(BasicAssetLinksTestData.MultipleStatementsJson())));

            // Act
            var isValid = await _sutProvider.Sut.ValidateAssetLinksAsync(_validRpId, _validPackageName, _validFingerprint);

            // Assert
            Assert.True(isValid);
        }
        
        [Fact]
        public async Task ValidateAssetLinksAsync_Throws_When_Data_Statement_Has_No_GetLoginCreds_Relation()
        {
            // Arrange
            _sutProvider.GetDependency<IApiService>()
                .GetDigitalAssetLinksForRpAsync(_validRpId)
                .Returns(Task.FromResult(Deserialize(BasicAssetLinksTestData.OneStatementNoGetLoginCredsRelationJson())));

            // Act
            var exception = await Assert.ThrowsAsync<Exceptions.ValidationException>(() => _sutProvider.Sut.ValidateAssetLinksAsync(_validRpId, _validPackageName, _validFingerprint));

            // Assert
            Assert.Equal(AppResources.PasskeyOperationFailedBecauseAppNotFoundInAssetLinks, exception.Message);
        }

        [Fact]
        public async Task ValidateAssetLinksAsync_Throws_When_Data_Statement_Has_No_HandleAllUrls_Relation()
        {
            // Arrange
            _sutProvider.GetDependency<IApiService>()
                .GetDigitalAssetLinksForRpAsync(_validRpId)
                .Returns(Task.FromResult(Deserialize(BasicAssetLinksTestData.OneStatementNoHandleAllUrlsRelationJson())));

            // Act
            var exception = await Assert.ThrowsAsync<Exceptions.ValidationException>(() => _sutProvider.Sut.ValidateAssetLinksAsync(_validRpId, _validPackageName, _validFingerprint));

            // Assert
            Assert.Equal(AppResources.PasskeyOperationFailedBecauseAppNotFoundInAssetLinks, exception.Message);
        }

        [Fact]
        public async Task ValidateAssetLinksAsync_Throws_When_Data_Statement_Has_Wrong_Namespace()
        {
            // Arrange
            _sutProvider.GetDependency<IApiService>()
                .GetDigitalAssetLinksForRpAsync(_validRpId)
                .Returns(Task.FromResult(Deserialize(BasicAssetLinksTestData.OneStatementWrongNamespaceJson())));

            // Act
            var exception = await Assert.ThrowsAsync<Exceptions.ValidationException>(() => _sutProvider.Sut.ValidateAssetLinksAsync(_validRpId, _validPackageName, _validFingerprint));

            // Assert
            Assert.Equal(AppResources.PasskeyOperationFailedBecauseAppNotFoundInAssetLinks, exception.Message);
        }

        [Fact]
        public async Task ValidateAssetLinksAsync_Throws_When_Data_Statement_Has_No_Fingerprints()
        {
            // Arrange
            _sutProvider.GetDependency<IApiService>()
                .GetDigitalAssetLinksForRpAsync(_validRpId)
                .Returns(Task.FromResult(Deserialize(BasicAssetLinksTestData.OneStatementNoFingerprintsJson())));

            // Act
            var exception = await Assert.ThrowsAsync<Exceptions.ValidationException>(() => _sutProvider.Sut.ValidateAssetLinksAsync(_validRpId, _validPackageName, _validFingerprint));

            // Assert
            Assert.Equal(AppResources.PasskeyOperationFailedBecauseAppCouldNotBeVerified, exception.Message);
        }

        [Fact]
        public async Task ValidateAssetLinksAsync_Throws_When_Data_PackageName_Doesnt_Match()
        {
            // Arrange
            _sutProvider.GetDependency<IApiService>()
                .GetDigitalAssetLinksForRpAsync(_validRpId)
                .Returns(Task.FromResult(Deserialize(BasicAssetLinksTestData.OneStatementOneFingerprintJson())));


            // Act
            var exception = await Assert.ThrowsAsync<Exceptions.ValidationException>(() => _sutProvider.Sut.ValidateAssetLinksAsync(_validRpId, "com.foo.another", _validFingerprint));

            // Assert
            Assert.Equal(AppResources.PasskeyOperationFailedBecauseAppNotFoundInAssetLinks, exception.Message);
        }

        [Fact]
        public async Task ValidateAssetLinksAsync_Throws_When_Data_Fingerprint_Doesnt_Match()
        {
            // Arrange
            _sutProvider.GetDependency<IApiService>()
                .GetDigitalAssetLinksForRpAsync(_validRpId)
                .Returns(Task.FromResult(Deserialize(BasicAssetLinksTestData.OneStatementOneFingerprintJson())));

            // Act
            var exception = await Assert.ThrowsAsync<Exceptions.ValidationException>(() => _sutProvider.Sut.ValidateAssetLinksAsync(_validRpId, _validPackageName, _validFingerprint.Replace("00", "33")));

            // Assert
            Assert.Equal(AppResources.PasskeyOperationFailedBecauseAppCouldNotBeVerified, exception.Message);
        }

        public void Dispose() {}
    }
}