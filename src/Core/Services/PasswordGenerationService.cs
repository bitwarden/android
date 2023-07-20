using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Threading.Tasks;
using Bit.Core.Abstractions;
using Bit.Core.Models.Domain;
using Bit.Core.Utilities;
using Zxcvbn;

namespace Bit.Core.Services
{
    public class PasswordGenerationService : IPasswordGenerationService
    {
        private const int MAX_PASSWORDS_IN_HISTORY = 100;
        private const string LOWERCASE_CHAR_SET = "abcdefghijkmnopqrstuvwxyz";
        private const string UPPERCASE_CHAR_SET = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        private const string NUMER_CHAR_SET = "23456789";
        private const string SPECIAL_CHAR_SET = "!@#$%^&*";

        private readonly ICryptoService _cryptoService;
        private readonly IStateService _stateService;
        private readonly ICryptoFunctionService _cryptoFunctionService;
        private readonly IPolicyService _policyService;
        private PasswordGenerationOptions _defaultOptions = PasswordGenerationOptions.CreateDefault;
        private PasswordGenerationOptions _optionsCache;
        private List<GeneratedPasswordHistory> _history;

        public PasswordGenerationService(
            ICryptoService cryptoService,
            IStateService stateService,
            ICryptoFunctionService cryptoFunctionService,
            IPolicyService policyService)
        {
            _cryptoService = cryptoService;
            _stateService = stateService;
            _cryptoFunctionService = cryptoFunctionService;
            _policyService = policyService;
        }

        public async Task<string> GeneratePasswordAsync(PasswordGenerationOptions options)
        {
            // Overload defaults with given options
            options.Merge(_defaultOptions);
            if (options.Type == PasswordGenerationOptions.TYPE_PASSPHRASE)
            {
                return await GeneratePassphraseAsync(options);
            }

            // Sanitize
            SanitizePasswordLength(options, true);

            var positionsBuilder = new StringBuilder();
            if (options.Lowercase.GetValueOrDefault() && options.MinLowercase > 0)
            {
                for (int i = 0; i < options.MinLowercase; i++)
                {
                    positionsBuilder.Append("l");
                }
            }
            if (options.Uppercase.GetValueOrDefault() && options.MinUppercase > 0)
            {
                for (int i = 0; i < options.MinUppercase; i++)
                {
                    positionsBuilder.Append("u");
                }
            }
            if (options.Number.GetValueOrDefault() && options.MinNumber > 0)
            {
                for (int i = 0; i < options.MinNumber; i++)
                {
                    positionsBuilder.Append("n");
                }
            }
            if (options.Special.GetValueOrDefault() && options.MinSpecial > 0)
            {
                for (int i = 0; i < options.MinSpecial; i++)
                {
                    positionsBuilder.Append("s");
                }
            }
            while (positionsBuilder.Length < options.Length.GetValueOrDefault())
            {
                positionsBuilder.Append("a");
            }

            // Shuffle
            var positions = positionsBuilder.ToString().ToCharArray()
                .OrderBy(a => _cryptoFunctionService.RandomNumber()).ToArray();

            // Build out other character sets
            var allCharSet = new StringBuilder();

            var lowercaseCharSet = LOWERCASE_CHAR_SET;
            if (options.AllowAmbiguousChar.GetValueOrDefault())
            {
                lowercaseCharSet = string.Concat(lowercaseCharSet, "l");
            }
            if (options.Lowercase.GetValueOrDefault())
            {
                allCharSet.Append(lowercaseCharSet);
            }

            var uppercaseCharSet = UPPERCASE_CHAR_SET;
            if (options.AllowAmbiguousChar.GetValueOrDefault())
            {
                uppercaseCharSet = string.Concat(uppercaseCharSet, "IO");
            }
            if (options.Uppercase.GetValueOrDefault())
            {
                allCharSet.Append(uppercaseCharSet);
            }

            var numberCharSet = NUMER_CHAR_SET;
            if (options.AllowAmbiguousChar.GetValueOrDefault())
            {
                numberCharSet = string.Concat(numberCharSet, "01");
            }
            if (options.Number.GetValueOrDefault())
            {
                allCharSet.Append(numberCharSet);
            }

            if (options.Special.GetValueOrDefault())
            {
                allCharSet.Append(SPECIAL_CHAR_SET);
            }

            var password = new StringBuilder();
            for (var i = 0; i < options.Length.GetValueOrDefault(); i++)
            {
                var charSetOnCurrentPosition = string.Empty;
                switch (positions[i])
                {
                    case 'l':
                        charSetOnCurrentPosition = lowercaseCharSet;
                        break;
                    case 'u':
                        charSetOnCurrentPosition = uppercaseCharSet;
                        break;
                    case 'n':
                        charSetOnCurrentPosition = numberCharSet;
                        break;
                    case 's':
                        charSetOnCurrentPosition = SPECIAL_CHAR_SET;
                        break;
                    case 'a':
                        charSetOnCurrentPosition = allCharSet.ToString();
                        break;
                }

                var randomCharIndex = await _cryptoService.RandomNumberAsync(0, charSetOnCurrentPosition.Length - 1);
                password.Append(charSetOnCurrentPosition[randomCharIndex]);
            }

            return password.ToString();
        }

        public void ClearCache()
        {
            _optionsCache = null;
            _history = null;
        }

        public async Task<string> GeneratePassphraseAsync(PasswordGenerationOptions options)
        {
            options.Merge(_defaultOptions);
            if (options.NumWords <= 2)
            {
                options.NumWords = _defaultOptions.NumWords;
            }
            if (options.WordSeparator == null || options.WordSeparator.Length == 0 || options.WordSeparator.Length > 1)
            {
                options.WordSeparator = " ";
            }
            if (options.Capitalize == null)
            {
                options.Capitalize = false;
            }
            if (options.IncludeNumber == null)
            {
                options.IncludeNumber = false;
            }
            var listLength = EEFLongWordList.Instance.List.Count - 1;
            var wordList = new List<string>();
            for (int i = 0; i < options.NumWords.GetValueOrDefault(); i++)
            {
                var wordIndex = await _cryptoService.RandomNumberAsync(0, listLength);
                if (options.Capitalize.GetValueOrDefault())
                {
                    wordList.Add(Capitalize(EEFLongWordList.Instance.List[wordIndex]));
                }
                else
                {
                    wordList.Add(EEFLongWordList.Instance.List[wordIndex]);
                }
            }
            if (options.IncludeNumber.GetValueOrDefault())
            {
                await AppendRandomNumberToRandomWordAsync(wordList);
            }
            return string.Join(options.WordSeparator, wordList);
        }

        public async Task<(PasswordGenerationOptions, PasswordGeneratorPolicyOptions)> GetOptionsAsync()
        {
            if (_optionsCache == null)
            {
                var options = await _stateService.GetPasswordGenerationOptionsAsync();
                if (options == null)
                {
                    _optionsCache = _defaultOptions;
                }
                else
                {
                    options.Merge(_defaultOptions);
                    _optionsCache = options;
                }
            }

            var policyOptions = await _policyService.GetPasswordGeneratorPolicyOptionsAsync();
            _optionsCache.EnforcePolicy(policyOptions);

            return (_optionsCache, policyOptions ?? new PasswordGeneratorPolicyOptions());
        }

        public List<string> GetPasswordStrengthUserInput(string email)
        {
            var atPosition = email?.IndexOf('@');
            if (atPosition is null || atPosition < 0)
            {
                return null;
            }
            var rx = new Regex("/[^A-Za-z0-9]/", RegexOptions.Compiled);
            var data = rx.Split(email.Substring(0, atPosition.Value).Trim().ToLower());

            return new List<string>(data);
        }

        public async Task SaveOptionsAsync(PasswordGenerationOptions options)
        {
            await _stateService.SetPasswordGenerationOptionsAsync(options);
            _optionsCache = options;
        }

        public async Task<List<GeneratedPasswordHistory>> GetHistoryAsync()
        {
            var hasKey = await _cryptoService.HasUserKeyAsync();
            if (!hasKey)
            {
                return new List<GeneratedPasswordHistory>();
            }
            if (_history == null)
            {
                var encrypted = await _stateService.GetEncryptedPasswordGenerationHistory();
                _history = await DecryptHistoryAsync(encrypted);
            }
            return _history ?? new List<GeneratedPasswordHistory>();
        }

        public async Task AddHistoryAsync(string password, CancellationToken token = default(CancellationToken))
        {
            var hasKey = await _cryptoService.HasUserKeyAsync();
            if (!hasKey)
            {
                return;
            }
            var currentHistory = await GetHistoryAsync();
            // Prevent duplicates
            if (MatchesPrevious(password, currentHistory))
            {
                return;
            }
            token.ThrowIfCancellationRequested();
            currentHistory.Insert(0, new GeneratedPasswordHistory { Password = password, Date = DateTime.UtcNow });
            // Remove old items.
            if (currentHistory.Count > MAX_PASSWORDS_IN_HISTORY)
            {
                currentHistory.RemoveAt(currentHistory.Count - 1);
            }
            var newHistory = await EncryptHistoryAsync(currentHistory);
            token.ThrowIfCancellationRequested();
            await _stateService.SetEncryptedPasswordGenerationHistoryAsync(newHistory);
        }

        public async Task ClearAsync(string userId = null)
        {
            _history = new List<GeneratedPasswordHistory>();
            await _stateService.SetEncryptedPasswordGenerationHistoryAsync(null, userId);
        }

        public Result PasswordStrength(string password, List<string> userInputs = null)
        {
            if (string.IsNullOrEmpty(password))
            {
                return null;
            }
            var globalUserInputs = new List<string>
            {
                "bitwarden",
                "bit",
                "warden"
            };
            if (userInputs != null && userInputs.Any())
            {
                globalUserInputs.AddRange(userInputs);
            }
            // Use a hash set to get rid of any duplicate user inputs
            var hashSet = new HashSet<string>(globalUserInputs);
            var finalUserInputs = new string[hashSet.Count];
            hashSet.CopyTo(finalUserInputs);
            var result = Zxcvbn.Core.EvaluatePassword(password, finalUserInputs);
            return result;
        }

        public void NormalizeOptions(PasswordGenerationOptions options,
            PasswordGeneratorPolicyOptions enforcedPolicyOptions)
        {
            options.MinLowercase = 0;
            options.MinUppercase = 0;

            if (!options.Uppercase.GetValueOrDefault() && !options.Lowercase.GetValueOrDefault() &&
                !options.Number.GetValueOrDefault() && !options.Special.GetValueOrDefault())
            {
                options.Lowercase = true;
            }

            var length = options.Length.GetValueOrDefault();
            if (length < 5)
            {
                options.Length = 5;
            }
            else if (length > 128)
            {
                options.Length = 128;
            }

            if (options.Length < enforcedPolicyOptions.MinLength)
            {
                options.Length = enforcedPolicyOptions.MinLength;
            }

            if (options.MinNumber == null)
            {
                options.MinNumber = 0;
            }
            else if (options.MinNumber > options.Length)
            {
                options.MinNumber = options.Length;
            }
            else if (options.MinNumber > 9)
            {
                options.MinNumber = 9;
            }

            if (options.MinNumber < enforcedPolicyOptions.NumberCount)
            {
                options.MinNumber = enforcedPolicyOptions.NumberCount;
            }

            if (options.MinSpecial == null)
            {
                options.MinSpecial = 0;
            }
            else if (options.MinSpecial > options.Length)
            {
                options.MinSpecial = options.Length;
            }
            else if (options.MinSpecial > 9)
            {
                options.MinSpecial = 9;
            }

            if (options.MinSpecial < enforcedPolicyOptions.SpecialCount)
            {
                options.MinSpecial = enforcedPolicyOptions.SpecialCount;
            }

            if (options.MinSpecial + options.MinNumber > options.Length)
            {
                options.MinSpecial = options.Length - options.MinNumber;
            }

            if (options.NumWords == null || options.Length < 3)
            {
                options.NumWords = 3;
            }
            else if (options.NumWords > 20)
            {
                options.NumWords = 20;
            }

            if (options.NumWords < enforcedPolicyOptions.MinNumberOfWords)
            {
                options.NumWords = enforcedPolicyOptions.MinNumberOfWords;
            }

            if (options.WordSeparator != null && options.WordSeparator.Length > 1)
            {
                options.WordSeparator = options.WordSeparator[0].ToString();
            }

            SanitizePasswordLength(options, false);
        }

        // Helpers

        private async Task<List<GeneratedPasswordHistory>> EncryptHistoryAsync(List<GeneratedPasswordHistory> history)
        {
            if (!history?.Any() ?? true)
            {
                return new List<GeneratedPasswordHistory>();
            }
            var tasks = history.Select(async item =>
            {
                if (item == null)
                {
                    return null;
                }
                var encrypted = await _cryptoService.EncryptAsync(item.Password);
                if (encrypted == null)
                {
                    return null;
                }
                return new GeneratedPasswordHistory
                {
                    Password = encrypted.EncryptedString,
                    Date = item.Date
                };
            });
            var h = await Task.WhenAll(tasks);
            return h.Where(x => x != null).ToList();
        }

        private async Task<List<GeneratedPasswordHistory>> DecryptHistoryAsync(List<GeneratedPasswordHistory> history)
        {
            if (!history?.Any() ?? true)
            {
                return new List<GeneratedPasswordHistory>();
            }
            var tasks = history.Select(async item =>
            {
                var decrypted = await _cryptoService.DecryptToUtf8Async(new EncString(item.Password));
                return new GeneratedPasswordHistory
                {
                    Password = decrypted,
                    Date = item.Date
                };
            });
            var h = await Task.WhenAll(tasks);
            return h.ToList();
        }

        private bool MatchesPrevious(string password, List<GeneratedPasswordHistory> history)
        {
            if (!history?.Any() ?? true)
            {
                return false;
            }
            return history.Last().Password == password;
        }

        private string Capitalize(string str)
        {
            return str.First().ToString().ToUpper() + str.Substring(1);
        }

        private async Task AppendRandomNumberToRandomWordAsync(List<string> wordList)
        {
            if (wordList == null || wordList.Count <= 0)
            {
                return;
            }
            var index = await _cryptoService.RandomNumberAsync(0, wordList.Count - 1);
            var num = await _cryptoService.RandomNumberAsync(0, 9);
            wordList[index] = wordList[index] + num;
        }

        private void SanitizePasswordLength(PasswordGenerationOptions options, bool forGeneration)
        {
            var minUppercaseCalc = 0;
            var minLowercaseCalc = 0;
            var minNumberCalc = options.MinNumber;
            var minSpecialCalc = options.MinNumber;

            if (options.Uppercase.GetValueOrDefault() && options.MinUppercase.GetValueOrDefault() <= 0)
            {
                minUppercaseCalc = 1;
            }
            else if (!options.Uppercase.GetValueOrDefault())
            {
                minUppercaseCalc = 0;
            }

            if (options.Lowercase.GetValueOrDefault() && options.MinLowercase.GetValueOrDefault() <= 0)
            {
                minLowercaseCalc = 1;
            }
            else if (!options.Lowercase.GetValueOrDefault())
            {
                minLowercaseCalc = 0;
            }

            if (options.Number.GetValueOrDefault() && options.MinNumber.GetValueOrDefault() <= 0)
            {
                minNumberCalc = 1;
            }
            else if (!options.Number.GetValueOrDefault())
            {
                minNumberCalc = 0;
            }

            if (options.Special.GetValueOrDefault() && options.MinSpecial.GetValueOrDefault() <= 0)
            {
                minSpecialCalc = 1;
            }
            else if (!options.Special.GetValueOrDefault())
            {
                minSpecialCalc = 0;
            }

            // This should never happen but is a final safety net
            if (options.Length.GetValueOrDefault() < 1)
            {
                options.Length = 10;
            }

            var minLength = minUppercaseCalc + minLowercaseCalc + minNumberCalc + minSpecialCalc;
            // Normalize and Generation both require this modification
            if (options.Length < minLength)
            {
                options.Length = minLength;
            }

            // Apply other changes if the options object passed in is for generation
            if (forGeneration)
            {
                options.MinUppercase = minUppercaseCalc;
                options.MinLowercase = minLowercaseCalc;
                options.MinNumber = minNumberCalc;
                options.MinSpecial = minSpecialCalc;
            }
        }
    }
}
