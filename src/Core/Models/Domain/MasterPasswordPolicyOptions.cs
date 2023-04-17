namespace Bit.Core.Models.Domain
{
    public class MasterPasswordPolicyOptions
    {
        public int MinComplexity { get; set; }
        public int MinLength { get; set; }
        public bool RequireUpper { get; set; }
        public bool RequireLower { get; set; }
        public bool RequireNumbers { get; set; }
        public bool RequireSpecial { get; set; }
        public bool EnforceOnLogin { get; set; }

        public bool InEffect()
        {
            return MinComplexity > 0 ||
                   MinLength > 0 ||
                   RequireUpper ||
                   RequireLower ||
                   RequireNumbers ||
                   RequireSpecial;
        }
    }
}
