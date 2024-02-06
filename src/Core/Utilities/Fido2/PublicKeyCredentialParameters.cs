namespace Bit.Core.Utilities.Fido2
{
    /// <summary>
    /// A description of a key type and algorithm.
    ///</example>
    public class PublicKeyCredentialParameters 
    {
        public string Type { get; set; }

        /// <summary>
        /// Cose algorithm identifier, e.g. -7 for ES256.
        /// </summary>
        public int Alg { get; set; }
    }
}
