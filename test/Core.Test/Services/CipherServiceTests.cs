using System;
using System.Net;
using System.Net.Http;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Exceptions;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Request;
using Bit.Core.Models.Response;
using Bit.Core.Models.View;
using Bit.Core.Services;
using Bit.Core.Test.AutoFixture;
using Bit.Test.Common.AutoFixture;
using Bit.Test.Common.AutoFixture.Attributes;
using NSubstitute;
using NSubstitute.ExceptionExtensions;
using Xunit;

namespace Bit.Core.Test.Services
{
    public class CipherServiceTests
    {
        [Theory, UserCipherAutoData]
        public async Task SaveWithServerAsync_PrefersFileUploadService(SutProvider<CipherService> sutProvider,
            Cipher cipher, string fileName, byte[] data, AttachmentUploadDataResponse uploadDataResponse, CipherString encKey)
        {
            sutProvider.GetDependency<ICryptoService>().EncryptAsync(fileName, Arg.Any<SymmetricCryptoKey>())
                .Returns(new CipherString(fileName));
            sutProvider.GetDependency<ICryptoService>().EncryptToBytesAsync(data, Arg.Any<SymmetricCryptoKey>())
                .Returns(data);
            sutProvider.GetDependency<ICryptoService>().MakeEncKeyAsync(Arg.Any<SymmetricCryptoKey>()).Returns(new Tuple<SymmetricCryptoKey, CipherString>(null, encKey));
            sutProvider.GetDependency<IApiService>().PostCipherAttachmentAsync(cipher.Id, Arg.Any<AttachmentRequest>())
                .Returns(uploadDataResponse);

            await sutProvider.Sut.SaveAttachmentRawWithServerAsync(cipher, fileName, data);

            await sutProvider.GetDependency<IFileUploadService>().Received(1)
                .UploadCipherAttachmentFileAsync(uploadDataResponse, fileName, data);
        }

        [Theory]
        [InlineUserCipherAutoData(HttpStatusCode.NotFound)]
        [InlineUserCipherAutoData(HttpStatusCode.MethodNotAllowed)]
        public async Task SaveWithServerAsync_FallsBackToLegacyFormData(HttpStatusCode statusCode,
            SutProvider<CipherService> sutProvider, Cipher cipher, string fileName, byte[] data,
            CipherResponse response, CipherString encKey)
        {
            sutProvider.GetDependency<ICryptoService>().EncryptAsync(fileName, Arg.Any<SymmetricCryptoKey>())
                .Returns(new CipherString(fileName));
            sutProvider.GetDependency<ICryptoService>().EncryptToBytesAsync(data, Arg.Any<SymmetricCryptoKey>())
                .Returns(data);
            sutProvider.GetDependency<ICryptoService>().MakeEncKeyAsync(Arg.Any<SymmetricCryptoKey>()).Returns(new Tuple<SymmetricCryptoKey, CipherString>(null, encKey));
            sutProvider.GetDependency<IApiService>().PostCipherAttachmentAsync(cipher.Id, Arg.Any<AttachmentRequest>())
                .Throws(new ApiException(new ErrorResponse {StatusCode = statusCode}));
            sutProvider.GetDependency<IApiService>().PostCipherAttachmentLegacyAsync(cipher.Id, Arg.Any<MultipartFormDataContent>())
                .Returns(response);

            await sutProvider.Sut.SaveAttachmentRawWithServerAsync(cipher, fileName, data);

            await sutProvider.GetDependency<IApiService>().Received(1)
                .PostCipherAttachmentLegacyAsync(cipher.Id, Arg.Any<MultipartFormDataContent>());
        }

        [Theory, UserCipherAutoData]
        public async Task SaveWithServerAsync_ThrowsOnBadRequestApiException(SutProvider<CipherService> sutProvider,
            Cipher cipher, string fileName, byte[] data, CipherString encKey)
        {
            sutProvider.GetDependency<ICryptoService>().EncryptAsync(fileName, Arg.Any<SymmetricCryptoKey>())
                .Returns(new CipherString(fileName));
            sutProvider.GetDependency<ICryptoService>().EncryptToBytesAsync(data, Arg.Any<SymmetricCryptoKey>())
                .Returns(data);
            sutProvider.GetDependency<ICryptoService>().MakeEncKeyAsync(Arg.Any<SymmetricCryptoKey>())
                .Returns(new Tuple<SymmetricCryptoKey, CipherString>(null, encKey));
            var expectedException = new ApiException(new ErrorResponse { StatusCode = HttpStatusCode.BadRequest });
            sutProvider.GetDependency<IApiService>().PostCipherAttachmentAsync(cipher.Id, Arg.Any<AttachmentRequest>())
                .Throws(expectedException);

            var actualException = await Assert.ThrowsAsync<ApiException>(async () => 
                await sutProvider.Sut.SaveAttachmentRawWithServerAsync(cipher, fileName, data));

            Assert.Equal(expectedException.Error.StatusCode, actualException.Error.StatusCode);
        }

        [Theory, CustomAutoData(typeof(SutProviderCustomization), typeof(SymmetricCryptoKeyCustomization))]
        public async Task DownloadAndDecryptAttachmentAsync_RequestsTimeLimitedUrl(SutProvider<CipherService> sutProvider,
            string cipherId, AttachmentView attachment, AttachmentResponse response)
        {
            sutProvider.GetDependency<IApiService>().GetAttachmentData(cipherId, attachment.Id)
                .Returns(response);

            await sutProvider.Sut.DownloadAndDecryptAttachmentAsync(cipherId, attachment, null);

            sutProvider.GetDependency<IApiService>().Received(1).GetAttachmentData(cipherId, attachment.Id);
        }
    }
}
