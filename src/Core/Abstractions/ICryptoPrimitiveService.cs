using Bit.Core.Enums;

namespace Bit.Core.Abstractions
{
    public interface ICryptoPrimitiveService
    {
        byte[] Pbkdf2(byte[] password, byte[] salt, CryptoHashAlgorithm algorithm, int iterations);
        byte[] Argon2id(byte[] password, byte[] salt, int iterations, int memory, int parallelism);
    }
}
