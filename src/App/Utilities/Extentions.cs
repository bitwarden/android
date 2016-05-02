using System;
using Bit.App.Abstractions;
using Bit.App.Models;
using XLabs.Ioc;

namespace Bit.App
{
    public static class Extentions
    {
        public static CipherString Encrypt(this string s)
        {
            if(s == null)
            {
                throw new ArgumentNullException(nameof(s));
            }

            var cryptoService = Resolver.Resolve<ICryptoService>();
            return cryptoService.Encrypt(s);
        }
    }
}
