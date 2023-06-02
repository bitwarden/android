using System;
using System.Text;
using AutoFixture;
using Bit.Core.Models.Domain;
using Bit.Core.Models.View;
using Bit.Test.Common.AutoFixture;
using Bit.Test.Common.AutoFixture.Attributes;

namespace Bit.Core.Test.AutoFixture

{
    internal class OrganizationCipher : ICustomization
    {
        public string OrganizationId { get; set; }
        public void Customize(IFixture fixture)
        {
            fixture.Customize<Cipher>(composer => composer
                .With(c => c.OrganizationId, OrganizationId ?? Guid.NewGuid().ToString()));
        }
    }

    internal class UserCipher : ICustomization
    {
        public void Customize(IFixture fixture)
        {
            fixture.Customize<Cipher>(composer => composer
                .Without(c => c.OrganizationId));
        }
    }

    internal class UserCipherView : ICustomization
    {
        public void Customize(IFixture fixture)
        {
            byte[] getRandomBytes(int size)
            {
                Random random = new Random();

                byte[] bytes = new byte[size];
                random.NextBytes(bytes);
                return bytes;
            };

            fixture.Customize<CipherView>(composer => composer
                .Without(c => c.OrganizationId)
                .Without(c => c.Attachments)
                .With(c => c.Key, new SymmetricCryptoKey(getRandomBytes(32), Enums.EncryptionType.AesCbc128_HmacSha256_B64)));
        }
    }

    internal class UserCipherAutoDataAttribute : CustomAutoDataAttribute
    {
        public UserCipherAutoDataAttribute() : base(new SutProviderCustomization(),
            new UserCipher(), new UserCipherView())
        { }
    }
    internal class InlineUserCipherAutoDataAttribute : InlineCustomAutoDataAttribute
    {
        public InlineUserCipherAutoDataAttribute(params object[] values) : base(new[] { typeof(SutProviderCustomization),
            typeof(UserCipher), typeof(UserCipherView) }, values)
        { }
    }

    internal class InlineKnownUserCipherAutoDataAttribute : InlineCustomAutoDataAttribute
    {
        public InlineKnownUserCipherAutoDataAttribute(string userId, params object[] values) : base(new ICustomization[]
            { new SutProviderCustomization(), new UserCipher() }, values)
        { }
    }

    internal class OrganizationCipherAutoDataAttribute : CustomAutoDataAttribute
    {
        public OrganizationCipherAutoDataAttribute(string organizationId = null) : base(new SutProviderCustomization(),
            new OrganizationCipher { OrganizationId = organizationId ?? null })
        { }
    }

    internal class InlineOrganizationCipherAutoDataAttribute : InlineCustomAutoDataAttribute
    {
        public InlineOrganizationCipherAutoDataAttribute(params object[] values) : base(new[] { typeof(SutProviderCustomization),
            typeof(OrganizationCipher) }, values)
        { }
    }
}
