using System;
using AutoFixture;
using Bit.Core.Models.Domain;
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

    internal class UserCipherAutoDataAttribute : CustomAutoDataAttribute
    {
        public UserCipherAutoDataAttribute() : base(new SutProviderCustomization(),
            new UserCipher())
        { }
    }
    internal class InlineUserCipherAutoDataAttribute : InlineCustomAutoDataAttribute
    {
        public InlineUserCipherAutoDataAttribute(params object[] values) : base(new[] { typeof(SutProviderCustomization),
            typeof(UserCipher) }, values)
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
