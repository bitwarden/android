using System;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Services;
using Bit.Core.Models.View;
using Bit.Core.Enums;
using Bit.Test.Common.AutoFixture;
using Bit.Test.Common.AutoFixture.Attributes;
using NSubstitute;
using Xunit;
using System.Collections.Generic;
using System.Linq;
using System.Diagnostics.CodeAnalysis;
using Bit.Core.Utilities;

namespace Bit.Core.Test.Services
{
    public class Fido2AuthenticatorSilentCredentialDiscoveryTests
    {
        [Theory]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization) })]
        public async Task SilentCredentialDiscoveryAsync_ReturnsEmptyArray_NoCredentialsExist(SutProvider<Fido2AuthenticatorService> sutProvider)
        {
            sutProvider.GetDependency<ICipherService>().GetAllDecryptedAsync().Returns(Task.FromResult(new List<CipherView>()));

            var result = await sutProvider.Sut.SilentCredentialDiscoveryAsync("bitwarden.com");

            Assert.Empty(result);
        }

        [Theory]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization) })]
        public async Task SilentCredentialDiscoveryAsync_ReturnsEmptyArray_OnlyNonDiscoverableCredentialsExist(SutProvider<Fido2AuthenticatorService> sutProvider)
        {
            sutProvider.GetDependency<ICipherService>().GetAllDecryptedAsync().Returns(Task.FromResult(new List<CipherView>
            {
                CreateCipherView("bitwarden.com", false),
                CreateCipherView("bitwarden.com", false),
                CreateCipherView("bitwarden.com", false)
            }));

            var result = await sutProvider.Sut.SilentCredentialDiscoveryAsync("bitwarden.com");

            Assert.Empty(result);
        }

        [Theory]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization) })]
        public async Task SilentCredentialDiscoveryAsync_ReturnsEmptyArray_NoCredentialsWithMatchingRpIdExist(SutProvider<Fido2AuthenticatorService> sutProvider)
        {
            sutProvider.GetDependency<ICipherService>().GetAllDecryptedAsync().Returns(Task.FromResult(new List<CipherView>
            {
                CreateCipherView("a.bitwarden.com", true),
                CreateCipherView("example.com", true)
            }));

            var result = await sutProvider.Sut.SilentCredentialDiscoveryAsync("bitwarden.com");

            Assert.Empty(result);
        }

        [Theory]
        [InlineCustomAutoData(new[] { typeof(SutProviderCustomization) })]
        public async Task SilentCredentialDiscoveryAsync_ReturnsCredentials_DiscoverableCredentialsWithMatchingRpIdExist(SutProvider<Fido2AuthenticatorService> sutProvider)
        {
            var matchingCredentials = new List<CipherView> {
                CreateCipherView("bitwarden.com", true),
                CreateCipherView("bitwarden.com", true)
            };
            var nonMatchingCredentials = new List<CipherView> {
                CreateCipherView("example.com", true)
            };
            sutProvider.GetDependency<ICipherService>().GetAllDecryptedAsync().Returns(
                matchingCredentials.Concat(nonMatchingCredentials).ToList()
            );

            var result = await sutProvider.Sut.SilentCredentialDiscoveryAsync("bitwarden.com");

            Assert.True(
                result.SequenceEqual(matchingCredentials.Select(c => new Fido2AuthenticatorDiscoverableCredentialMetadata {
                    Type = Constants.DefaultFido2CredentialType,
                    Id = c.Login.MainFido2Credential.CredentialId.GuidToRawFormat(),
                    RpId = "bitwarden.com",
                    UserHandle = c.Login.MainFido2Credential.UserHandleValue,
                    UserName = c.Login.MainFido2Credential.UserName,
                    CipherId = c.Id,
                }), new MetadataComparer())
            );
        }

        private byte[] RandomBytes(int length)
        {
            var bytes = new byte[length];
            new Random().NextBytes(bytes);
            return bytes;
        }

        #nullable enable
        private CipherView CreateCipherView(string rpId, bool discoverable)
        {
            return new CipherView {
                Type = CipherType.Login,
                Id = Guid.NewGuid().ToString(),
                Reprompt = CipherRepromptType.None,
                Login = new LoginView {
                    Fido2Credentials = new List<Fido2CredentialView> {
                        new Fido2CredentialView {
                            CredentialId = Guid.NewGuid().ToString(),
                            RpId = rpId ?? "null.com",
                            DiscoverableValue = discoverable,
                            UserHandleValue = RandomBytes(32),
                            KeyValue = "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgO4wC7AlY4eJP7uedRUJGYsAIJAd6gN1Vp7uJh6xXAp6hRANCAARGvr56F_t27DEG1Tzl-qJRhrTUtC7jOEbasAEEZcE3TiMqoWCan0sxKDPylhRYk-1qyrBC_feN1UtGWH57sROa"
                        }
                    }
                }
            };
        }

        private class MetadataComparer : IEqualityComparer<Fido2AuthenticatorDiscoverableCredentialMetadata>
        {
            public int GetHashCode([DisallowNull] Fido2AuthenticatorDiscoverableCredentialMetadata obj) => throw new NotImplementedException();

            public bool Equals(Fido2AuthenticatorDiscoverableCredentialMetadata? a, Fido2AuthenticatorDiscoverableCredentialMetadata? b) =>
                a != null && b != null && a.Type == b.Type && a.RpId == b.RpId && a.UserName == b.UserName && a.Id.SequenceEqual(b.Id) && a.UserHandle.SequenceEqual(b.UserHandle);
        }
    }
}
