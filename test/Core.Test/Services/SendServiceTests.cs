using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Models.Data;
using Bit.Core.Models.Domain;
using Bit.Core.Models.Response;
using Bit.Core.Services;
using Bit.Core.Utilities;
using Bit.Core.Enums;
using Bit.Test.Common;
using Bit.Test.Common.AutoFixture;
using Bit.Test.Common.AutoFixture.Attributes;
using Newtonsoft.Json;
using NSubstitute;
using Xunit;
using System.Text;
using System.Net.Http;
using Bit.Core.Models.Request;
using Bit.Core.Test.AutoFixture;
using System.Linq.Expressions;
using Bit.Core.Models.View;

namespace Bit.Core.Test.Services
{
    public class SendServiceTests
    {
        private string SEND_KEY(string userId) => SendService.SEND_KEY(userId);

        [Theory]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization), typeof(TextSendCustomization) })]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization), typeof(FileSendCustomization) })]
        public async Task ReplaceAsync_Success(SutProvider<SendService> sutProvider, string userId, IEnumerable<SendData> sendDatas)
        {
            var actualSendDataDict = sendDatas.ToDictionary(d => d.Id, d => d);
            sutProvider.GetDependency<IUserService>().GetUserIdAsync().Returns(userId);

            await sutProvider.Sut.ReplaceAsync(actualSendDataDict);

            await sutProvider.GetDependency<IStorageService>()
                .Received(1).SaveAsync(SEND_KEY(userId), actualSendDataDict);
        }

        [Theory]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization), typeof(TextSendCustomization) }, 0)]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization), typeof(TextSendCustomization) }, 1)]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization), typeof(TextSendCustomization) }, 2)]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization), typeof(TextSendCustomization) }, 3)]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization), typeof(TextSendCustomization) }, 4)]
        public async Task DeleteAsync_Success(int numberToDelete, SutProvider<SendService> sutProvider, string userId, IEnumerable<SendData> sendDatas)
        {
            var actualSendDataDict = sendDatas.ToDictionary(d => d.Id, d => d);
            sutProvider.GetDependency<IUserService>().GetUserIdAsync().Returns(userId);
            sutProvider.GetDependency<IStorageService>()
                .GetAsync<Dictionary<string, SendData>>(SEND_KEY(userId)).Returns(actualSendDataDict);

            var idsToDelete = actualSendDataDict.Take(numberToDelete).Select(kvp => kvp.Key).ToArray();
            var expectedSends = actualSendDataDict.Skip(numberToDelete).ToDictionary(kvp => kvp.Key, kvp => kvp.Value);

            await sutProvider.Sut.DeleteAsync(idsToDelete);


            await sutProvider.GetDependency<IStorageService>().Received(1)
                .SaveAsync(SEND_KEY(userId),
                    Arg.Is<Dictionary<string, SendData>>(s => TestHelper.AssertEqualExpectedPredicate(expectedSends)(s)));
        }

        [Theory, SutAutoData]
        public async Task ClearAsync_Success(SutProvider<SendService> sutProvider, string userId)
        {
            await sutProvider.Sut.ClearAsync(userId);

            await sutProvider.GetDependency<IStorageService>().Received(1).RemoveAsync(SEND_KEY(userId));
        }

        [Theory]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization), typeof(TextSendCustomization) })]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization), typeof(FileSendCustomization) })]
        public async Task DeleteWithServerAsync_Success(SutProvider<SendService> sutProvider, string userId, IEnumerable<SendData> sendDatas)
        {
            var initialSendDatas = sendDatas.ToDictionary(d => d.Id, d => d);
            var idToDelete = initialSendDatas.First().Key;
            var expectedSends = initialSendDatas.Skip(1).ToDictionary(kvp => kvp.Key, kvp => kvp.Value);
            sutProvider.GetDependency<IUserService>().GetUserIdAsync().Returns(userId);
            sutProvider.GetDependency<IStorageService>()
                .GetAsync<Dictionary<string, SendData>>(Arg.Any<string>()).Returns(initialSendDatas);

            await sutProvider.Sut.DeleteWithServerAsync(idToDelete);

            await sutProvider.GetDependency<IApiService>().Received(1).DeleteSendAsync(idToDelete);
            await sutProvider.GetDependency<IStorageService>().Received(1)
                .SaveAsync(SEND_KEY(userId),
                    Arg.Is<Dictionary<string, SendData>>(s => TestHelper.AssertEqualExpectedPredicate(expectedSends)(s)));
        }

        [Theory]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization), typeof(TextSendCustomization) })]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization), typeof(FileSendCustomization) })]
        public async Task GetAsync_Success(SutProvider<SendService> sutProvider, string userId, IEnumerable<SendData> sendDatas)
        {
            var sendDataDict = sendDatas.ToDictionary(d => d.Id, d => d);
            sutProvider.GetDependency<IUserService>().GetUserIdAsync().Returns(userId);
            sutProvider.GetDependency<IStorageService>().GetAsync<Dictionary<string, SendData>>(SEND_KEY(userId)).Returns(sendDataDict);

            foreach (var dataKvp in sendDataDict)
            {
                var expected = new Send(dataKvp.Value);
                var actual = await sutProvider.Sut.GetAsync(dataKvp.Key);
                TestHelper.AssertPropertyEqual(expected, actual);
            }
        }

        [Theory, SutAutoData]
        public async Task GetAsync_NonExistringId_ReturnsNull(SutProvider<SendService> sutProvider, string userId, IEnumerable<SendData> sendDatas)
        {
            var sendDataDict = sendDatas.ToDictionary(d => d.Id, d => d);
            sutProvider.GetDependency<IUserService>().GetUserIdAsync().Returns(userId);
            sutProvider.GetDependency<IStorageService>().GetAsync<Dictionary<string, SendData>>(SEND_KEY(userId)).Returns(sendDataDict);

            var actual = await sutProvider.Sut.GetAsync(Guid.NewGuid().ToString());

            Assert.Null(actual);
        }

        [Theory]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization), typeof(TextSendCustomization) })]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization), typeof(FileSendCustomization) })]
        public async Task GetAllAsync_Success(SutProvider<SendService> sutProvider, string userId, IEnumerable<SendData> sendDatas)
        {
            var sendDataDict = sendDatas.ToDictionary(d => d.Id, d => d);
            sutProvider.GetDependency<IUserService>().GetUserIdAsync().Returns(userId);
            sutProvider.GetDependency<IStorageService>().GetAsync<Dictionary<string, SendData>>(SEND_KEY(userId)).Returns(sendDataDict);

            var allExpected = sendDataDict.Select(kvp => new Send(kvp.Value));
            var allActual = await sutProvider.Sut.GetAllAsync();
            foreach (var (actual, expected) in allActual.Zip(allExpected))
            {
                TestHelper.AssertPropertyEqual(expected, actual);
            }
        }

        [Theory, SutAutoData]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization), typeof(TextSendCustomization) })]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization), typeof(FileSendCustomization) })]
        public async Task GetAllDecryptedAsync_Success(SutProvider<SendService> sutProvider, string userId, IEnumerable<SendData> sendDatas)
        {
            var sendDataDict = sendDatas.ToDictionary(d => d.Id, d => d);
            sutProvider.GetDependency<ICryptoService>().HasKeyAsync().Returns(true);
            ServiceContainer.Register("cryptoService", sutProvider.GetDependency<ICryptoService>());
            sutProvider.GetDependency<II18nService>().StringComparer.Returns(StringComparer.CurrentCulture);
            sutProvider.GetDependency<IUserService>().GetUserIdAsync().Returns(userId);
            sutProvider.GetDependency<IStorageService>().GetAsync<Dictionary<string, SendData>>(SEND_KEY(userId)).Returns(sendDataDict);

            var actual = await sutProvider.Sut.GetAllDecryptedAsync();

            Assert.Equal(sendDataDict.Count, actual.Count);
            foreach (var (actualView, expectedId) in actual.Zip(sendDataDict.Select(s => s.Key)))
            {
                // Note Send -> SendView is tested in SendTests
                Assert.Equal(expectedId, actualView.Id);
            }

            ServiceContainer.Reset();
        }

        // SaveWithServer()
        [Theory]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization), typeof(TextSendCustomization) })]
        public async Task SaveWithServerAsync_NewTextSend_Success(SutProvider<SendService> sutProvider, string userId, SendResponse response, Send send)
        {
            send.Id = null;
            sutProvider.GetDependency<IUserService>().GetUserIdAsync().Returns(userId);
            sutProvider.GetDependency<IApiService>().PostSendAsync(Arg.Any<SendRequest>()).Returns(response);
            sutProvider.GetDependency<IApiService>().PostSendFileAsync(Arg.Any<MultipartFormDataContent>()).Returns(response);

            var fileContentBytes = Encoding.UTF8.GetBytes("This is the file content");

            await sutProvider.Sut.SaveWithServerAsync(send, fileContentBytes);

            Predicate<SendRequest> sendRequestPredicate = r =>
            {
                // Note Send -> SendRequest tested in SendRequestTests
                TestHelper.AssertPropertyEqual(new SendRequest(send), r);
                return true;
            };

            switch (send.Type)
            {
                case SendType.Text:
                    await sutProvider.GetDependency<IApiService>().Received(1)
                        .PostSendAsync(Arg.Is<SendRequest>(r => sendRequestPredicate(r)));
                    break;
                case SendType.File:
                default:
                    throw new Exception("Untested send type");
            }
        }


        [Theory]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization), typeof(FileSendCustomization) })]
        public async Task SaveWithServerAsync_NewFileSend_Success(SutProvider<SendService> sutProvider, string userId, SendResponse response, Send send)
        {
            send.Id = null;
            sutProvider.GetDependency<IUserService>().GetUserIdAsync().Returns(userId);
            sutProvider.GetDependency<IApiService>().PostSendAsync(Arg.Any<SendRequest>()).Returns(response);
            sutProvider.GetDependency<IApiService>().PostSendFileAsync(Arg.Any<MultipartFormDataContent>()).Returns(response);

            var fileContentBytes = Encoding.UTF8.GetBytes("This is the file content");

            await sutProvider.Sut.SaveWithServerAsync(send, fileContentBytes);

            Predicate<MultipartFormDataContent> formDataPredicate = fd =>
            {
                Assert.Equal(2, fd.Count()); // expect a request and file content

                var expectedRequest = JsonConvert.SerializeObject(new SendRequest(send));
                var actualRequest = fd.First().ReadAsStringAsync().GetAwaiter().GetResult();
                Assert.Equal(expectedRequest, actualRequest);

                var actualFileContent = fd.Skip(1).First().ReadAsByteArrayAsync().GetAwaiter().GetResult();
                Assert.Equal(fileContentBytes, actualFileContent);
                return true;
            };

            switch (send.Type)
            {
                case SendType.File:
                    await sutProvider.GetDependency<IApiService>().Received(1)
                        .PostSendFileAsync(Arg.Is<MultipartFormDataContent>(f => formDataPredicate(f)));
                    break;
                case SendType.Text:
                default:
                    throw new Exception("Untested send type");
            }
        }

        [Theory]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization), typeof(TextSendCustomization) })]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization), typeof(FileSendCustomization) })]
        public async Task SaveWithServerAsync_PutSend_Success(SutProvider<SendService> sutProvider, string userId, SendResponse response, Send send)
        {
            sutProvider.GetDependency<IUserService>().GetUserIdAsync().Returns(userId);
            sutProvider.GetDependency<IApiService>().PutSendAsync(send.Id, Arg.Any<SendRequest>()).Returns(response);

            await sutProvider.Sut.SaveWithServerAsync(send, null);

            Predicate<SendRequest> sendRequestPredicate = r =>
            {
                // Note Send -> SendRequest tested in SendRequestTests
                TestHelper.AssertPropertyEqual(new SendRequest(send), r);
                return true;
            };

            await sutProvider.GetDependency<IApiService>().Received(1)
                .PutSendAsync(send.Id, Arg.Is<SendRequest>(r => sendRequestPredicate(r)));
        }

        [Theory, SutAutoData]
        public async Task RemovePasswordWithServerAsync_Success(SutProvider<SendService> sutProvider, SendResponse response, string sendId)
        {
            sutProvider.GetDependency<IApiService>().PutSendRemovePasswordAsync(sendId).Returns(response);

            await sutProvider.Sut.RemovePasswordWithServerAsync(sendId);

            await sutProvider.GetDependency<IApiService>().Received(1).PutSendRemovePasswordAsync(sendId);
            await sutProvider.GetDependency<IStorageService>().ReceivedWithAnyArgs(1).SaveAsync<Dictionary<string, SendData>>(default, default);
        }

        [Theory]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization), typeof(TextSendCustomization) })]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization), typeof(FileSendCustomization) })]
        public async Task UpsertAsync_Update_Success(SutProvider<SendService> sutProvider, string userId, IEnumerable<SendData> initialSends)
        {
            var initialSendDict = initialSends.ToDictionary(s => s.Id, s => s);
            sutProvider.GetDependency<IUserService>().GetUserIdAsync().Returns(userId);
            sutProvider.GetDependency<IStorageService>().GetAsync<Dictionary<string, SendData>>(SEND_KEY(userId)).Returns(initialSendDict);

            var updatedSends = CoreHelpers.Clone(initialSendDict);
            foreach (var kvp in updatedSends)
            {
                kvp.Value.Disabled = !kvp.Value.Disabled;
            }

            await sutProvider.Sut.UpsertAsync(updatedSends.Values.ToArray());

            Predicate<Dictionary<string, SendData>> matchSendsPredicate = actual =>
            {
                Assert.Equal(updatedSends.Count, actual.Count);
                foreach (var (expectedKvp, actualKvp) in updatedSends.Zip(actual))
                {
                    Assert.Equal(expectedKvp.Key, actualKvp.Key);
                    TestHelper.AssertPropertyEqual(expectedKvp.Value, actualKvp.Value);
                }
                return true;
            };
            await sutProvider.GetDependency<IStorageService>().Received(1).SaveAsync(SEND_KEY(userId), Arg.Is<Dictionary<string, SendData>>(d => matchSendsPredicate(d)));
        }

        [Theory]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization), typeof(TextSendCustomization) })]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization), typeof(FileSendCustomization) })]
        public async Task UpsertAsync_NewSends_Success(SutProvider<SendService> sutProvider, string userId, IEnumerable<SendData> initialSends, IEnumerable<SendData> newSends)
        {
            var initialSendDict = initialSends.ToDictionary(s => s.Id, s => s);
            sutProvider.GetDependency<IUserService>().GetUserIdAsync().Returns(userId);
            sutProvider.GetDependency<IStorageService>().GetAsync<Dictionary<string, SendData>>(SEND_KEY(userId)).Returns(initialSendDict);

            var expectedDict = CoreHelpers.Clone(initialSendDict).Concat(newSends.Select(s => new KeyValuePair<string, SendData>(s.Id, s)));

            await sutProvider.Sut.UpsertAsync(newSends.ToArray());

            Predicate<Dictionary<string, SendData>> matchSendsPredicate = actual =>
            {
                Assert.Equal(expectedDict.Count(), actual.Count);
                foreach (var (expectedKvp, actualKvp) in expectedDict.Zip(actual))
                {
                    Assert.Equal(expectedKvp.Key, actualKvp.Key);
                    TestHelper.AssertPropertyEqual(expectedKvp.Value, actualKvp.Value);
                }
                return true;
            };
            await sutProvider.GetDependency<IStorageService>().Received(1).SaveAsync(SEND_KEY(userId), Arg.Is<Dictionary<string, SendData>>(d => matchSendsPredicate(d)));
        }

        // TODO test encrypt
        [Theory]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization), typeof(SymmetricCryptoKeyCustomization), typeof(TextSendCustomization) })]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization), typeof(SymmetricCryptoKeyCustomization), typeof(FileSendCustomization) })]
        public async Task EncryptAsync_Success(SutProvider<SendService> sutProvider, SendView view, byte[] fileData, SymmetricCryptoKey privateKey)
        {
            var prefix = "encrypted_";
            var prefixBytes = Encoding.UTF8.GetBytes(prefix);


            byte[] getPbkdf(string password, byte[] key)
                => prefixBytes.Concat(Encoding.UTF8.GetBytes(password)).Concat(key).ToArray();
            CipherString encryptBytes(byte[] secret, SymmetricCryptoKey key)
                => new CipherString($"{prefix}{Convert.ToBase64String(secret)}{Convert.ToBase64String(key.Key)}");
            CipherString encrypt(string secret, SymmetricCryptoKey key)
                => new CipherString($"{prefix}{secret}{Convert.ToBase64String(key.Key)}");

            sutProvider.GetDependency<ICryptoFunctionService>().Pbkdf2Async(Arg.Any<string>(), Arg.Any<byte[]>(), Arg.Any<CryptoHashAlgorithm>(), Arg.Any<int>())
                .Returns(info => getPbkdf((string)info[0], (byte[])info[1]));
            sutProvider.GetDependency<ICryptoService>().EncryptAsync(Arg.Any<byte[]>(), Arg.Any<SymmetricCryptoKey>())
                .Returns(info => encryptBytes((byte[])info[0], (SymmetricCryptoKey)info[1]));
            sutProvider.GetDependency<ICryptoService>().EncryptAsync(Arg.Any<string>(), Arg.Any<SymmetricCryptoKey>())
                .Returns(info => encrypt((string)info[0], (SymmetricCryptoKey)info[1]));

            var (send, encryptedFileData) = await sutProvider.Sut.EncryptAsync(view, fileData, view.Password, privateKey);

            TestHelper.AssertPropertyEqual(view, send, "Password", "Key", "Name", "Notes", "Text", "File",
                "AccessCount", "AccessId", "CryptoKey", "RevisionDate", "DeletionDate", "ExpirationDate", "UrlB64Key",
                "MaxAccessCountReached", "Expired", "PendingDelete");
            Assert.Equal(Convert.ToBase64String(getPbkdf(view.Password, view.Key)), send.Password);
            TestHelper.AssertPropertyEqual(encryptBytes(view.Key, privateKey), send.Key);
            TestHelper.AssertPropertyEqual(encrypt(view.Name, view.CryptoKey), send.Name);
            TestHelper.AssertPropertyEqual(encrypt(view.Notes, view.CryptoKey), send.Notes);

            switch (view.Type)
            {
                case SendType.Text:
                    TestHelper.AssertPropertyEqual(view.Text, send.Text, "Text", "MaskedText");
                    TestHelper.AssertPropertyEqual(encrypt(view.Text.Text, view.CryptoKey), send.Text.Text);
                    break;
                case SendType.File:
                    // Only set filename
                    TestHelper.AssertPropertyEqual(encrypt(view.File.FileName, view.CryptoKey), send.File.FileName);
                    TestHelper.AssertPropertyEqual(encryptBytes(fileData, view.CryptoKey), encryptedFileData);
                    break;
                default:
                    throw new Exception("Untested send type");
            }
        }
    }
}
