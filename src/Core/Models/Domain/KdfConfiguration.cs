using Bit.Core;
using Bit.Core.Enums;
using Bit.Core.Models.Domain;
public struct KdfConfig
{
    public static KdfConfig Default = new KdfConfig(KdfType.PBKDF2_SHA256, 5000, null, null);
    public KdfConfig(KdfType? type, int? iterations, int? memory, int? parallelism)
    {
        Type = type;
        Iterations = iterations;
        Memory = memory;
        Parallelism = parallelism;
    }

    public KdfConfig(Account.AccountProfile profile)
    {
        Type = profile.KdfType;
        Iterations = profile.KdfIterations;
        Memory = profile.KdfMemory;
        Parallelism = profile.KdfParallelism;
    }

    public KdfType? Type { get; set; }
    public int? Iterations { get; set; }
    public int? Memory { get; set; }
    public int? Parallelism { get; set; }
}
