using System;
using AutoFixture;
using Bit.Core.Models.Domain;
using Bit.Core.Services;

namespace Bit.Core.Test.AutoFixture
{
    public class SymmetricCryptoKeyCustomization : ICustomization
    {
        public void Customize(IFixture fixture)
        {
            var keyMaterial = (new CryptoFunctionService(null)).RandomBytes(32);
            fixture.Register(() => new SymmetricCryptoKey(keyMaterial));
        }
    }
}
