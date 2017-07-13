using System;

namespace Bit.App.Utilities
{
    // ref: https://github.com/aspnet/Identity/blob/dev/src/Microsoft.Extensions.Identity.Core/Base32.cs
    // with some modifications for cleaning input
    public static class Base32
    {
        private static readonly string _base32Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

        public static byte[] FromBase32(string input)
        {
            if(input == null)
            {
                throw new ArgumentNullException(nameof(input));
            }

            input = input.ToUpperInvariant();
            var cleanedInput = string.Empty;
            foreach(var c in input)
            {
                if(_base32Chars.IndexOf(c) < 0)
                {
                    continue;
                }

                cleanedInput += c;
            }

            input = cleanedInput;
            if(input.Length == 0)
            {
                return new byte[0];
            }

            var output = new byte[input.Length * 5 / 8];
            var bitIndex = 0;
            var inputIndex = 0;
            var outputBits = 0;
            var outputIndex = 0;

            while(outputIndex < output.Length)
            {
                var byteIndex = _base32Chars.IndexOf(input[inputIndex]);
                if(byteIndex < 0)
                {
                    throw new FormatException();
                }

                var bits = Math.Min(5 - bitIndex, 8 - outputBits);
                output[outputIndex] <<= bits;
                output[outputIndex] |= (byte)(byteIndex >> (5 - (bitIndex + bits)));

                bitIndex += bits;
                if(bitIndex >= 5)
                {
                    inputIndex++;
                    bitIndex = 0;
                }

                outputBits += bits;
                if(outputBits >= 8)
                {
                    outputIndex++;
                    outputBits = 0;
                }
            }

            return output;
        }
    }
}
