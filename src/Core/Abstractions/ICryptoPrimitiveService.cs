using Bit.Core.Enums;

namespace Bit.Core.Abstractions
{
    public interface ICryptoPrimitiveService
    {
        byte[] Pbkdf2(byte[] password, byte[] salt, CryptoHashAlgorithm algorithm, int iterations);
    }
}
